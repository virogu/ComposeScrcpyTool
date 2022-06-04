package com.virogu.tools.config

import com.virogu.bean.Configs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

abstract class ScrcpyConfig : HistoryDeviceConfigTool() {

    companion object {
        private const val KEY = "KEY_SCRCPY_CONFIG"
    }

    override val scrcpyConfigFlow: StateFlow<Map<String, Configs.ScrcpyConfig>> = configsFlow.map {
        it.getConfigNotNull(KEY, emptyMap<String, Configs.ScrcpyConfig>())
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

}