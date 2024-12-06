/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.device.ability.adb

import com.virogu.core.bean.Additional
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class AndroidDeviceAdditionalAbility(private val device: Device) : DeviceAbilityAdditional() {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val localFormatTime get() = LocalDateTime.now().format(timeFormatter)
    private val target = arrayOf("-s", device.serial)

    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
        private val logger = KotlinLogging.logger { }
    }

    override suspend fun exec(additional: Additional): String {
        try {
            val commands = when (additional) {
                Additional.StatusBar -> listOf(arrayOf("shell", "input keyevent 83"))
                Additional.PowerButton -> listOf(arrayOf("shell", "input keyevent 26"))
                Additional.VolumePlus -> listOf(arrayOf("shell", "input keyevent 24"))
                Additional.VolumeReduce -> listOf(arrayOf("shell", "input keyevent 25"))
                Additional.TaskManagement -> listOf(arrayOf("shell", "input keyevent 187"))
                Additional.Menu -> listOf(arrayOf("shell", "input keyevent 82"))
                Additional.Home -> listOf(arrayOf("shell", "input keyevent 3"))
                Additional.Back -> listOf(arrayOf("shell", "input keyevent 4"))
                Additional.ScreenShot -> {
                    return doSnapshot()
                }
            }
            commands.forEach { command ->
                cmd.adb(*target, *command, consoleLog = true)
                delay(20)
            }
            return ""
        } catch (e: Throwable) {
            logger.warn { e.localizedMessage }
            return "操作失败: ${e.localizedMessage}"
        }
    }

    private suspend fun doSnapshot(): String {
        val saveDir = getScreenSavePath()
        val fileName = "IMG_${localFormatTime}.png"
        val screenFile = File(saveDir, fileName).absolutePath
        val screen = cmd.adb(*target, "exec-out", "screencap", "-p", ">", screenFile).getOrNull() ?: "error"
        if (screen.isNotEmpty()) {
            return "截图失败: $screen"
        }
        return "截图已保存至 $screenFile"
    }
}