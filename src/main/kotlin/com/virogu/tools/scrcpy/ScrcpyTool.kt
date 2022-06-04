package com.virogu.tools.scrcpy

import com.virogu.bean.Configs
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyTool {
    val isBusy: StateFlow<Boolean>
    val activeDevicesFLow: StateFlow<Set<String>>

    fun connect(serial: String, config: Configs.ScrcpyConfig = Configs.ScrcpyConfig())

    fun disConnect(serial: String? = null)

}