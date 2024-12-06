/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.virogu.core.bean.SimpleConfig
import com.virogu.core.config.BaseConfigStore
import com.virogu.core.config.SimpleConfigStore
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