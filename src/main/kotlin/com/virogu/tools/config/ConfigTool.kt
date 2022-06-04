package com.virogu.tools.config

import com.virogu.bean.Configs
import kotlinx.coroutines.flow.StateFlow

interface ConfigTool {

    val configsFlow: StateFlow<Map<String, String>>

    val deviceDescFlow: StateFlow<Map<String, String>>

    val historyDeviceFlow: StateFlow<List<Configs.HistoryDevice>>

    val scrcpyConfigFlow: StateFlow<Map<String, Configs.ScrcpyConfig>>

    fun updateDesc(device: String, desc: String)

    fun updateLastConnect(device: Configs.HistoryDevice)

    fun updateLastConnectTagged(device: Configs.HistoryDevice, tagged: Boolean)

    fun removeLastConnect(device: Configs.HistoryDevice)

    fun clearHistoryConnect()

}