package com.virogu.core.config.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.config.BaseConfigStore
import com.virogu.core.config.ScrcpyConfigStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ScrcpyConfigImpl(
    dataStore: DataStore<Preferences>
) : BaseConfigStore(dataStore), ScrcpyConfigStore {

    companion object {
        private val KEY = stringPreferencesKey("KEY_SCRCPY_CONFIG")
    }

    override val scrcpyConfigFlow: StateFlow<ScrcpyConfig> = getSerializableConfig(
        KEY, ScrcpyConfig()
    ).stateIn(scope, SharingStarted.Eagerly, ScrcpyConfig())

    override fun updateScrcpyConfig(config: ScrcpyConfig.CommonConfig) {
        val current = scrcpyConfigFlow.value
        if (config == current.commonConfig) {
            return
        }
        val new = current.copy(
            commonConfig = config
        )
        updateSerializableConfig(KEY, new)
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
        updateSerializableConfig(KEY, new)
    }

    override fun removeScrcpyConfig(serial: String) {
        val current = scrcpyConfigFlow.value
        if (!current.configs.containsKey(serial)) {
            return
        }
        val configs = current.configs.toMutableMap()
        configs.remove(serial)
        val new = current.copy(configs = configs)
        updateSerializableConfig(KEY, new)
    }

    override fun clearConfig() {
        updateSerializableConfig(KEY, ScrcpyConfig())
    }
}