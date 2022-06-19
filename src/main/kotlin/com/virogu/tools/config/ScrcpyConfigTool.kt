package com.virogu.tools.config

import com.virogu.bean.ScrcpyConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

abstract class ScrcpyConfigTool : HistoryDeviceConfigTool() {

    companion object {
        private const val KEY = "KEY_SCRCPY_CONFIG"
    }

    override val scrcpyConfigFlow: StateFlow<ScrcpyConfig> = configsFlow.map {
        it.getConfigNotNull(KEY, ScrcpyConfig())
    }.stateIn(scope, SharingStarted.Eagerly, ScrcpyConfig())

    override fun updateScrcpyConfig(config: ScrcpyConfig.CommonConfig) {
        val current = scrcpyConfigFlow.value
        if (config == current.commonConfig) {
            println("new common config is same as before, return.")
            return
        }
        val new = current.copy(
            commonConfig = config
        )
        updateConfig(KEY, new)
    }

    override fun updateScrcpyConfig(serial: String, config: ScrcpyConfig.Config) {
        val current = scrcpyConfigFlow.value
        val configs = current.configs.apply {
            if (get(serial) == config) {
                return
            }
        }.toMutableMap().apply {
            put(serial, config)
        }
        val new = current.copy(
            configs = configs
        )
        updateConfig(KEY, new)
    }

}