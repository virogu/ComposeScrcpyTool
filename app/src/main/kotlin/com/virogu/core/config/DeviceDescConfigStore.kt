package com.virogu.core.config

import kotlinx.coroutines.flow.StateFlow

interface DeviceDescConfigStore {

    val deviceDescFlow: StateFlow<Map<String, String>>

    fun updateDesc(device: String, desc: String)

    fun removeDesc(device: String)

    fun clearDesc()

}