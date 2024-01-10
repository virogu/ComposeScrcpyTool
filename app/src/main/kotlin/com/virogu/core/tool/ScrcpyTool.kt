package com.virogu.core.tool

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.ScrcpyConfig
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyTool {
    val isBusy: StateFlow<Boolean>
    val activeDevicesFLow: StateFlow<Set<String>>

    fun connect(
        device: DeviceInfo,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    )

    fun disConnect(device: DeviceInfo? = null)

}