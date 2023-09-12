package com.virogu.tools.config

import kotlinx.coroutines.flow.StateFlow

interface DeviceDescConfigStore {

    val deviceDescFlow: StateFlow<Map<String, String>>

    fun updateDesc(device: String, desc: String)

}