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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.util.concurrent.ConcurrentHashMap

/**
 * 设备端提权方式。探测一次后整个会话内复用，避免每条命令都盲试 `su 0 -c || su -c || ...` 三连回退。
 */
enum class SuMode {
    /** 设备无 su 或探测超时（如 Magisk 授权弹窗未处理），命令以 shell 用户直跑 */
    NONE,

    /** Magisk / KernelSU / AOSP su 通用形式：su 0 sh -c 'cmd' */
    SU0,

    /** 旧 SuperSU 风格：su -c 'cmd' */
    SU_C,
}

/**
 * 按设备 serial 缓存的能力画像。
 * [com.virogu.core.device.DeviceEntityAndroid] 在每次设备列表刷新时都会重建实例，
 * 缓存必须放在实例之外才能跨刷新存活。
 */
object AdbDeviceProfileCache {

    private val cache = ConcurrentHashMap<String, CompletableDeferred<SuMode>>()
    private val probeMutex = Mutex()

    private val cmd: AdbCommand by DI.global.instance<AdbCommand>()

    suspend fun suMode(serial: String): SuMode {
        val deferred = cache.computeIfAbsent(serial) { CompletableDeferred() }
        if (deferred.isCompleted) {
            return deferred.await()
        }
        return probeMutex.withLock {
            if (deferred.isCompleted) {
                deferred.await()
            } else {
                val mode = probe(serial)
                deferred.complete(mode)
                mode
            }
        }
    }

    private suspend fun probe(serial: String): SuMode {
        // 先用 command -v 判断 su 是否存在，避免在无 root 的设备上产生无谓的错误输出。
        // SU0 的探测形式必须是 `su 0 sh -c`：Android 5 的 AOSP su 对 `su 0 -c id` 会
        // 静默失败，对 `su -c <串>` 只做 execv 不经 shell，只有 argv 透传形式全版本可用。
        // 两段探测分别对应 Magisk/KernelSU（su 0 sh -c）与旧 SuperSU（su -c）。
        // stderr 抑制仅用于探测本身，业务命令的错误不会被丢弃。
        val probeCmd = "if command -v su >/dev/null 2>&1; then " +
                "echo \"U0:$(su 0 sh -c id 2>/dev/null)\"; " +
                "echo \"UC:$(su -c id 2>/dev/null)\"; " +
                "else echo NOSU; fi"
        val out = cmd.adbChecked("-s", serial, "shell", probeCmd, timeout = 15L, consoleLog = false)
            .getOrNull()?.output.orEmpty()
        return when {
            out.lineByPrefix("U0:")?.contains("uid=0") == true -> SuMode.SU0
            out.lineByPrefix("UC:")?.contains("uid=0") == true -> SuMode.SU_C
            else -> SuMode.NONE
        }
    }

    private fun String.lineByPrefix(prefix: String): String? =
        lineSequence().firstOrNull { it.startsWith(prefix) }
}
