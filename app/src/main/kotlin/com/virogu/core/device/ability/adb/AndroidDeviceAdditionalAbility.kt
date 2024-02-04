package com.virogu.core.device.ability.adb

import com.virogu.core.bean.Additional
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class AndroidDeviceAdditionalAbility(device: Device) : DeviceAbilityAdditional {
    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
    }

    override suspend fun exec(additional: Additional) {
        val command = when (additional) {
            Additional.StatusBar -> arrayOf("shell", "input keyevent 83")
            Additional.PowerButton -> arrayOf("shell", "input keyevent 26")
            Additional.VolumePlus -> arrayOf("shell", "input keyevent 24")
            Additional.VolumeReduce -> arrayOf("shell", "input keyevent 25")
            Additional.TaskManagement -> arrayOf("shell", "input keyevent 187")
            Additional.Menu -> arrayOf("shell", "input keyevent 82")
            Additional.Home -> arrayOf("shell", "input keyevent 3")
            Additional.Back -> arrayOf("shell", "input keyevent 4")
        }
        cmd.adb(*command, showLog = true)
    }
}