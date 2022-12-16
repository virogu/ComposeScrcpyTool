package com.virogu.tools.config

import com.virogu.bean.Configs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ConfigToolImpl : ScrcpyConfigTool() {

    companion object {
        private const val KEY_SIMPLE_CONFIG = "KEY_SIMPLE_CONFIG"
    }

    override val simpleConfig: StateFlow<Configs.SimpleConfig> = configsFlow.map {
        it.getConfigNotNull(KEY_SIMPLE_CONFIG, Configs.SimpleConfig())
    }.stateIn(scope, SharingStarted.Eagerly, Configs.SimpleConfig())

    override fun updateSimpleConfig(config: Configs.SimpleConfig) {
        val current = simpleConfig.value
        if (config == current) {
            return
        }
        updateConfig(KEY_SIMPLE_CONFIG, config)
    }

}