package com.virogu.tools.config.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.virogu.bean.HistoryDevice
import com.virogu.tools.config.BaseConfigStore
import com.virogu.tools.config.HistoryDevicesStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryDeviceConfigImpl(
    dataStore: DataStore<Preferences>
) : BaseConfigStore(dataStore), HistoryDevicesStore {
    companion object {
        private val KEY = stringPreferencesKey("KEY_HISTORY_DEVICE")
    }

    private val deviceListCompare = Comparator<HistoryDevice> { o1, o2 ->
        if (o1.tagged == o2.tagged) {
            o2.timeMs.compareTo(o1.timeMs)
        } else if (o1.tagged) {
            -1
        } else {
            1
        }
    }

    override val historyDeviceFlow: StateFlow<List<HistoryDevice>> = getSerializableConfig(
        KEY, emptyList<HistoryDevice>()
    ).map {
        it.sortedWith(deviceListCompare)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun updateLastConnect(device: HistoryDevice) {
        val history = historyDeviceFlow.value
        val new = history.toMutableList().apply {
            val current = this.find {
                it.ip == device.ip
            }
            this.removeIf {
                it.ip == device.ip
            }
            if (current != null) {
                add(0, current.copy(timeMs = device.timeMs))
            } else {
                add(0, device)
            }
        }
        updateSerializableConfig(KEY, new)
    }

    override fun updateLastConnectTagged(device: HistoryDevice, tagged: Boolean) {
        val history = historyDeviceFlow.value
        val new = history.toMutableList().apply {
            val index = this.indexOfFirst {
                it.ip == device.ip
            }
            if (index !in this.indices) {
                return
            }
            val current = this.removeAt(index)
            add(index, current.copy(tagged = tagged))
        }
        updateSerializableConfig(KEY, new)
    }

    override fun removeLastConnect(device: HistoryDevice) {
        val history = historyDeviceFlow.value
        val new = history.toMutableList().apply {
            val r = removeIf {
                it.ip == device.ip && it.port == device.port
            }
            if (!r) {
                return
            }
        }
        updateSerializableConfig(KEY, new)
    }

    override fun clearHistoryConnect() {
        clearConfig(KEY)
    }

}