package com.virogu.tools

import com.virogu.bean.ScrcpyConfig
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyTool {
    val isBusy: StateFlow<Boolean>
    val activeDevicesFLow: StateFlow<Set<String>>

    fun connect(
        serial: String,
        title: String,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    )

    fun disConnect(serial: String? = null)

}