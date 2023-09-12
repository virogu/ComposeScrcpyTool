package com.virogu.tools.config.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.virogu.bean.SimpleConfig
import com.virogu.tools.config.BaseConfigStore
import com.virogu.tools.config.SimpleConfigStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SimpleConfigImpl(
    dataStore: DataStore<Preferences>
) : BaseConfigStore(dataStore), SimpleConfigStore {

    companion object {
        private val KEY = stringPreferencesKey("KEY_SIMPLE_CONFIG")
    }

    override val simpleConfig: StateFlow<SimpleConfig> = getSerializableConfig(
        KEY, SimpleConfig()
    ).stateIn(scope, SharingStarted.Eagerly, SimpleConfig())

    override fun updateSimpleConfig(config: SimpleConfig) {
        val current = simpleConfig.value
        if (config == current) {
            return
        }
        updateSerializableConfig(KEY, config)
    }

}