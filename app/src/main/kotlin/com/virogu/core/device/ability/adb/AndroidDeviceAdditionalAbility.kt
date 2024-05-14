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
    private val serial = device.serial
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val localFormatTime get() = LocalDateTime.now().format(timeFormatter)

    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override suspend fun exec(additional: Additional) {
        try {
            val commands = when (additional) {
                Additional.StatusBar -> listOf(arrayOf("-s", serial, "shell", "input keyevent 83"))
                Additional.PowerButton -> listOf(arrayOf("-s", serial, "shell", "input keyevent 26"))
                Additional.VolumePlus -> listOf(arrayOf("-s", serial, "shell", "input keyevent 24"))
                Additional.VolumeReduce -> listOf(arrayOf("-s", serial, "shell", "input keyevent 25"))
                Additional.TaskManagement -> listOf(arrayOf("-s", serial, "shell", "input keyevent 187"))
                Additional.Menu -> listOf(arrayOf("-s", serial, "shell", "input keyevent 82"))
                Additional.Home -> listOf(arrayOf("-s", serial, "shell", "input keyevent 3"))
                Additional.Back -> listOf(arrayOf("-s", serial, "shell", "input keyevent 4"))
                Additional.ScreenShot -> {
                    doSnapshot()
                    return
                }
            }
            commands.forEach { command ->
                cmd.adb(*command, consoleLog = true)
                delay(20)
            }
        } catch (e: Throwable) {
            logger.warn(e.localizedMessage)
        }
    }

    private suspend fun doSnapshot() {
        val saveDir = getScreenSavePath()
        val screenFile = "/sdcard/IMG_${localFormatTime}.png"
        cmd.adb("-s", serial, "shell", "screencap", "-p", screenFile)
        val item = FileInfoItem(path = screenFile, type = FileType.FILE)
        device.folderAbility.pullFile(listOf(item), saveDir)
        device.folderAbility.deleteFile(item)
    }
}