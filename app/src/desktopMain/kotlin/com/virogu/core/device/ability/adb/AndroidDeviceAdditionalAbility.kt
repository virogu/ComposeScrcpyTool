package com.virogu.core.device.ability.adb

import com.virogu.core.bean.Additional
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
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
            logger.warn(e.localizedMessage)
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