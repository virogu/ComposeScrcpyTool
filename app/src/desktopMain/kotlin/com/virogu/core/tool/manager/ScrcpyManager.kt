package com.virogu.core.tool.manager

import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.device.Device
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyManager {
    val isBusy: StateFlow<Boolean>
    val activeDevicesFLow: StateFlow<Set<String>>

    fun connect(
        device: Device,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    )

    fun disConnect(device: Device? = null)

}