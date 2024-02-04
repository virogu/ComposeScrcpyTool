package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityScrcpy
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceScrcpyAbility(device: Device) : DeviceAbilityScrcpy {
    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override suspend fun connect(
        scope: CoroutineScope,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ): Process? {
        logger.warn("当前设备暂不支持")
        return null
    }
}