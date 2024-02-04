package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.ProcessInfo
import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityProcess
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceProcessAbility(device: Device) : DeviceAbilityProcess {
    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
    }

    override suspend fun refresh(): List<ProcessInfo> {
        return emptyList()
    }

    override suspend fun killProcess(info: ProcessInfo): Result<String> {
        return Result.success("")
    }

    override suspend fun forceStopProcess(info: ProcessInfo): Result<String> {
        return Result.success("")
    }
}