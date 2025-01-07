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

package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.Additional
import com.virogu.core.bean.Additional.*
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileType
import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceAdditionalAbility(private val device: Device) : DeviceAbilityAdditional() {

    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
        private val logger = KotlinLogging.logger { }
    }

    private val target = arrayOf("-t", device.serial)
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val localFormatTime get() = LocalDateTime.now().format(timeFormatter)

    //@ohos.multimodalInput.keyCode
    //https://docs.openharmony.cn/pages/v5.0/zh-cn/application-dev/reference/apis-input-kit/js-apis-keycode.md
    override suspend fun exec(additional: Additional): String {
        try {
            val commands: List<Array<String>> = when (additional) {
                ScreenShot -> {
                    return doSnapshot()
                }

                StatusBar -> listOf()
                PowerButton -> listOf(arrayOf("shell", "uinput -K -d 18 -u 18"))
                VolumePlus -> listOf(arrayOf("shell", "uinput -K -d 16 -u 16"))
                VolumeReduce -> listOf(arrayOf("shell", "uinput -K -d 17 -u 17"))
                TaskManagement -> listOf(arrayOf("shell", "uinput -K -d 2078 -u 2078"))
                Menu -> listOf(arrayOf("shell", "uinput -K -d 2067 -u 2067"))
                Home -> listOf(arrayOf("shell", "uinput -K -d 1 -u 1"))
                Back -> listOf(arrayOf("shell", "uinput -K -d 2 -u 2"))
            }
            commands.forEach { command ->
                cmd.hdc(*target, *command, consoleLog = true)
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
        val fileName = "IMG_${localFormatTime}.jpeg"
        val screenFile = "/data/local/tmp/$fileName"
        val r = cmd.hdc(*target, "shell", "snapshot_display", "-f", screenFile, consoleLog = true).getOrNull().orEmpty()
        if (!r.contains("success", ignoreCase = true)) {
            if (r.contains("error, 13", ignoreCase = true)) {
                return "截图失败, 可能临时目录/data/local/tmp被删除，请重启设备"
            }
            return "截图失败: $r"
        }
        //val regex = Regex("""/\S+\.jpeg""")
        //val matchResult = regex.find(r)
        //val screenFile = matchResult?.value ?: throw IllegalStateException("截图失败: $r")
        val item = FileInfoItem(path = screenFile, type = FileType.FILE)
        device.folderAbility.pullFile(listOf(item), saveDir)
        device.folderAbility.deleteFile(item)
        return "截图已保存至 ${saveDir.path}\\$fileName"
    }

}