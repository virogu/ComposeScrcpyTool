package com.virogu.tools.config

import com.virogu.bean.ScrcpyConfig
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyConfigStore {

    val scrcpyConfigFlow: StateFlow<ScrcpyConfig>

    fun updateScrcpyConfig(config: ScrcpyConfig.CommonConfig)

    fun updateScrcpyConfig(serial: String, config: ScrcpyConfig.Config)

}