package com.virogu.core.config

import com.virogu.core.bean.ScrcpyConfig
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyConfigStore {

    val scrcpyConfigFlow: StateFlow<ScrcpyConfig>

    fun updateScrcpyConfig(config: ScrcpyConfig.CommonConfig)

    fun updateScrcpyConfig(serial: String, config: ScrcpyConfig.Config)

    fun removeScrcpyConfig(serial: String)

    fun clearConfig()

}