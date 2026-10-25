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

import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.process.ProcessInfo
import com.virogu.core.device.process.ProcessInfoAndroid
import com.virogu.core.device.process.ProcessListParser

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class AndroidDeviceProcessAbility(device: Device) : DeviceAbilityProcess {

    private val shell = AdbShellExecutor(device.serial)

    override suspend fun refresh(): Result<List<ProcessInfo>> {
        // 无参数的 ps 在 toolbox（Android 5/6）与 toybox（Android 7+）上都输出全部进程，
        // 首列 USER/次列 PID/末列 NAME 两代一致，由 ProcessListParser 过滤应用进程。
        // 不能用 `ps -A || ps` 回退：toolbox 的 ps -A 不报错，而是把 -A 当进程名过滤器，
        // 输出仅表头且退出码 0，回退链永远不会触发。
        // 旧实现 `am dump -a | grep 'PID #'` 在 Android 5 上没有 am dump 子命令、
        // 输出数 MB 且受 5 秒超时限制，失败一律静默成空列表。
        return shell.execNoSu("ps", consoleLog = false, timeout = 20L).mapCatching { sr ->
            if (sr.exitCode != 0) {
                throw IllegalStateException(sr.output.ifEmpty { "ps 执行失败 (exit ${sr.exitCode})" })
            }
            ProcessListParser.parseAppProcesses(sr.output.lines()).map {
                ProcessInfoAndroid(
                    user = it.user,
                    uid = it.user,
                    pid = it.pid,
                    processName = it.processName,
                    packageName = it.processName.substringBefore(':'),
                )
            }
        }
    }

    override suspend fun killProcess(info: ProcessInfo): Result<String> {
        // `am kill` 只杀"安全可杀"的后台进程，前台/常驻进程会被静默跳过且退出码仍为 0，
        // 表现为"不生效"；改为直接对选中的 PID 发信号（toolbox/toybox 的 kill 均可用，
        // 对 :子进程 也能精确停止）。shell 用户被 SELinux 拒绝时按设备画像走 su 重试。
        // 注意：前台 Activity 的进程被 kill 后系统会立即重建（列表里出现新 PID），
        // 要彻底停止整个应用需使用 force-stop，这是系统行为。
        val pid = info.pid
        val direct = shell.execNoSu("kill $pid", timeout = 10L).getOrNull()
        if (direct != null && direct.exitCode == 0) {
            return Result.success("kill $pid success")
        }
        return shell.exec("kill $pid", timeout = 10L).mapCatching { sr ->
            when {
                // 进程已退出视为达成目的
                sr.exitCode == 0 || sr.output.contains("No such process", ignoreCase = true) ->
                    sr.output.trim().ifEmpty { "kill $pid success" }
                else -> throw IllegalStateException(
                    sr.output.ifEmpty { "停止进程失败 (exit ${sr.exitCode})" } +
                            "；无 root 权限时无法停止前台进程，可改用「强行停止」"
                )
            }
        }
    }

    override suspend fun forceStopProcess(info: ProcessInfo): Result<String> =
        execAm("am force-stop ${info.packageName}")

    /** am 系列命令出错时打印 Error: 文本但退出码为 0，需要一并判定 */
    private suspend fun execAm(commandStr: String): Result<String> =
        shell.execNoSu(commandStr, timeout = 15L).mapCatching { sr ->
            val output = sr.output.trim()
            if (sr.exitCode != 0 || output.startsWith("Error", ignoreCase = true)) {
                throw IllegalStateException(output.ifEmpty { "执行失败 (exit ${sr.exitCode})" })
            }
            output
        }
}
