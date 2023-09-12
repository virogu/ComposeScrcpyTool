package com.virogu.tools.config.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.virogu.tools.config.BaseConfigStore
import com.virogu.tools.config.DeviceDescConfigStore
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

}