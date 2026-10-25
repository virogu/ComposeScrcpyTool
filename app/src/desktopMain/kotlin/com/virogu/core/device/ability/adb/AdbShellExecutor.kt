/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.device.ability.adb

import com.virogu.core.command.AdbCommand
import com.virogu.core.command.DeviceShell
import com.virogu.core.command.ShellResult
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * 单台 Android 设备的 shell 执行器：su 包装 + 退出码标记解析，folder/process 等 ability 共用。
 */
internal class AdbShellExecutor(private val serial: String) {

    private val target = arrayOf("-s", serial)

    private val cmd: AdbCommand by DI.global.instance<AdbCommand>()

    /**
     * su 感知执行：按设备画像（[AdbDeviceProfileCache] 探测一次）包装 su。
     * 命令链的 stderr 不被吞掉，失败原因可透出。
     */
    suspend fun exec(commandStr: String, consoleLog: Boolean = true, timeout: Long = 30L): Result<ShellResult> {
        val su = AdbDeviceProfileCache.suMode(serial)
        if (su == SuMode.NONE) {
            return execNoSu(commandStr, consoleLog, timeout)
        }
        // 退出码标记必须包进 su 内层、由内层 sh 求值：su 不透传子命令退出码，
        // 标记放外层时任何失败都会被 su 自身的退出码 0 掩盖成假成功
        val marked = DeviceShell.wrapWithExitMarker(commandStr)
        val body = when (su) {
            // Android 5 的 AOSP su 对 `su -c <串>` 只做 execv、不经过 shell，
            // 整条命令链会报 "exec failed ... No such file or directory"；
            // 显式经 sh -c 执行在 AOSP su / Magisk / KernelSU 上都兼容
            SuMode.SU0 -> "su 0 sh -c '${DeviceShell.escapeSingleQuotes(marked)}'"
            SuMode.SU_C -> "su -c '${DeviceShell.escapeSingleQuotes(marked)}'"
            SuMode.NONE -> marked
        }
        return cmd.adbChecked(*target, "shell", body, consoleLog = consoleLog, timeout = timeout)
            .parseMarker()
    }

    /** 不经 su 包装的执行（ps、am、kill 等以 shell 用户即可的命令） */
    suspend fun execNoSu(commandStr: String, consoleLog: Boolean = true, timeout: Long = 30L): Result<ShellResult> =
        cmd.adbChecked(
            *target,
            "shell",
            DeviceShell.wrapWithExitMarker(commandStr),
            consoleLog = consoleLog,
            timeout = timeout
        ).parseMarker()

    private fun Result<ShellResult>.parseMarker(): Result<ShellResult> = mapCatching { sr ->
        val parsed = DeviceShell.parseShellResult(sr.output)
        if (parsed.exitCode < 0) {
            // 没拿到退出码标记：su 被拒、设备掉线等，必须报错而不是当空结果
            throw IllegalStateException(
                parsed.output.ifEmpty { "设备无响应或命令未执行 (exit ${sr.exitCode})" }
            )
        }
        parsed
    }
}
