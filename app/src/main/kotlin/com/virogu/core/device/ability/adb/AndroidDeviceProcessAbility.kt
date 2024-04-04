package com.virogu.core.device.ability.adb

import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.process.ProcessInfo
import com.virogu.core.device.process.ProcessInfoAndroid
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class AndroidDeviceProcessAbility(private val device: Device) : DeviceAbilityProcess {
    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
    }

    override suspend fun refresh(): List<ProcessInfo> {
        return cmd.adb(
            "-s", device.serial, "shell", "dumpsys activity processes"
        ).map {
            ProcessInfoAndroid.parse(it)
        }.getOrNull().orEmpty()
    }

    override suspend fun killProcess(info: ProcessInfo): Result<String> {
        return cmd.adb(
            "-s", device.serial, "shell",
            "am", "kill", info.packageName,
            consoleLog = true
        )
    }

    override suspend fun forceStopProcess(info: ProcessInfo): Result<String> {
        return cmd.adb(
            "-s", device.serial, "shell",
            "am", "force-stop", info.packageName,
            consoleLog = true
        )
    }

}