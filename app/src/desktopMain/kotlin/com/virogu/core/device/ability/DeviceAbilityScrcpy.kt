package com.virogu.core.device.ability

import com.virogu.core.bean.ScrcpyConfig
import kotlinx.coroutines.CoroutineScope

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:35
 **/
interface DeviceAbilityScrcpy {
    suspend fun connect(
        scope: CoroutineScope,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ): Process?
}