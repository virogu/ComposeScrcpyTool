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
import com.virogu.core.config.BaseConfigStore
import com.virogu.core.config.DeviceDescConfigStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DescConfigImpl(
    dataStore: DataStore<Preferences>
) : BaseConfigStore(dataStore), DeviceDescConfigStore {

    companion object {
        private val KEY = stringPreferencesKey("KEY_DEVICE_DESC")
    }

    override val deviceDescFlow: StateFlow<Map<String, String>> = getSerializableConfig(
        KEY, emptyMap<String, String>()
    ).stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override fun updateDesc(device: String, desc: String) {
        val deviceDesc = deviceDescFlow.value
        val newDesc = deviceDesc.apply {
            if (this[device] == desc) {
                return
            }
        }.toMutableMap().apply {
            put(device, desc)
        }
        updateSerializableConfig(KEY, newDesc)
    }

    override fun removeDesc(device: String) {
        val deviceDesc = deviceDescFlow.value
        if (!deviceDesc.contains(device)) {
            return
        }
        val newDesc = deviceDesc.toMutableMap()
        newDesc.remove(device)
        updateSerializableConfig(KEY, newDesc)
    }

    override fun clearDesc() {
        updateSerializableConfig(KEY, emptyMap<String, String>())
    }

}