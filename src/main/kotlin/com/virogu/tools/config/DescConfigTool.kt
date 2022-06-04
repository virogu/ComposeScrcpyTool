package com.virogu.tools.config

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

abstract class DescConfigTool : BaseConfigTool() {

    companion object {
        private const val KEY = "KEY_DEVICE_DESC"
    }

    override val deviceDescFlow: StateFlow<Map<String, String>> = configsFlow.map {
        it.getConfigNotNull<Map<String, String>>(KEY, emptyMap())
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override fun updateDesc(device: String, desc: String) {
        val deviceDesc = deviceDescFlow.value
        val newDesc = deviceDesc.apply {
            if (this[device] == desc) {
                return
            }
        }.toMutableMap().apply {
            put(device, desc)
        }
        updateConfig(KEY, newDesc)
    }


}