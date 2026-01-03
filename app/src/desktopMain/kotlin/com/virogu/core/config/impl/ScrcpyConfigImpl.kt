/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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