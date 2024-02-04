package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.Additional
import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityAdditional
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceAdditionalAbility(device: Device) : DeviceAbilityAdditional {
    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
    }

    override suspend fun exec(additional: Additional) {

    }
}