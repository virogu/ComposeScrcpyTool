package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.Additional
import com.virogu.core.bean.Additional.*
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileType
import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceAdditionalAbility(private val device: Device) : DeviceAbilityAdditional() {
    private val serial = device.serial

    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    //@ohos.multimodalInput.keyCode
    override suspend fun exec(additional: Additional) {
        try {
            val commands: List<Array<String>> = when (additional) {
                ScreenShot -> {
                    doSnapshot()
                    return
                }

                StatusBar -> listOf()
                PowerButton -> listOf(arrayOf("shell", "uinput -K -d 18 -u 18"))
                VolumePlus -> listOf(arrayOf("shell", "uinput -K -d 16 -u 16"))
                VolumeReduce -> listOf(arrayOf("shell", "uinput -K -d 17 -u 17"))
                TaskManagement -> listOf(arrayOf("shell", "uinput -K -d 2717 -u 2717"))
                Menu -> listOf(arrayOf("shell", "uinput -K -d 2067 -u 2067"))
                Home -> listOf(arrayOf("shell", "uinput -K -d 1 -u 1"))
                Back -> listOf(arrayOf("shell", "uinput -K -d 2 -u 2"))
            }
            commands.forEach { command ->
                cmd.hdc("-t", serial, *command, consoleLog = true)
                delay(20)
            }
        } catch (e: Throwable) {
            logger.warn(e.localizedMessage)
        }
    }

    private suspend fun doSnapshot() {
        val saveDir = getScreenSavePath()
        val r = cmd.hdc("-t", serial, "shell", "snapshot_display").getOrThrow()
        val regex = Regex("/data/snapshot_[\\d-]+_[\\d-]+.jpeg")
        val matchResult = regex.find(r)
        val screenFile = matchResult?.value ?: throw IllegalStateException("截图失败: $r")
        val item = FileInfoItem(path = screenFile, type = FileType.FILE)
        device.folderAbility.pullFile(listOf(item), saveDir)
        device.folderAbility.deleteFile(item)
    }

}