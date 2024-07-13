package com.virogu.core.device.ability.adb

import com.virogu.core.bean.Additional
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileType
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        //cmd.adb(*target, "shell", "screencap", redirectFile = File(saveDir, fileName), autoDeleteRedirectFile = false)
        val screenFile = "/data/local/tmp/$fileName"
        val r = cmd.adb(*target, "shell", "screencap", "-p", screenFile, consoleLog = true).getOrNull() ?: "error"
        if (r.contains("error", ignoreCase = true)) {
            if (r.contains("No such file or directory", ignoreCase = true)) {
                return "截图失败, 可能临时目录/data/local/tmp被删除，请重启设备"
            }
            return "截图失败: $r"
        }
        val item = FileInfoItem(path = screenFile, type = FileType.FILE)
        device.folderAbility.pullFile(listOf(item), saveDir)
        device.folderAbility.deleteFile(item)
        return "截图已保存至 ${saveDir.path}"
    }
}