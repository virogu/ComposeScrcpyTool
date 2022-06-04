package com.virogu.tools.config

import com.virogu.bean.Configs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

abstract class HistoryDeviceConfigTool : DescConfigTool() {
    companion object {
        private const val KEY = "KEY_HISTORY_DEVICE"
    }

    private val deviceListCompare = Comparator<Configs.HistoryDevice> { o1, o2 ->
        if (o1.tagged == o2.tagged) {
            o2.timeMs.compareTo(o1.timeMs)
        } else if (o1.tagged) {
            -1
        } else {
            1
        }
    }

    override val historyDeviceFlow: StateFlow<List<Configs.HistoryDevice>> = configsFlow.map {
        it.getConfigNotNull(
            KEY,
            emptyList<Configs.HistoryDevice>()
        ).sortedWith(deviceListCompare)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun updateLastConnect(device: Configs.HistoryDevice) {
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
        updateConfig(KEY, new)
    }

    override fun updateLastConnectTagged(device: Configs.HistoryDevice, tagged: Boolean) {
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
        updateConfig(KEY, new)
    }

    override fun removeLastConnect(device: Configs.HistoryDevice) {
        val history = historyDeviceFlow.value
        val new = history.toMutableList().apply {
            val r = removeIf {
                it.ip == device.ip && it.port == device.port
            }
            if (!r) {
                return
            }
        }
        updateConfig(KEY, new)
    }

    override fun clearHistoryConnect() {
        updateConfig(KEY, emptyList<Configs.HistoryDevice>())
    }

}