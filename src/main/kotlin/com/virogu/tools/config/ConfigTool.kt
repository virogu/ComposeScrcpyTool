package com.virogu.tools.config

import com.virogu.bean.Configs
import com.virogu.bean.ScrcpyConfig
import kotlinx.coroutines.flow.StateFlow

interface ConfigTool {

    val configsFlow: StateFlow<Map<String, String>>

    val deviceDescFlow: StateFlow<Map<String, String>>

    val historyDeviceFlow: StateFlow<List<Configs.HistoryDevice>>

    val scrcpyConfigFlow: StateFlow<ScrcpyConfig>

    val simpleConfig: StateFlow<Configs.SimpleConfig>

    fun writeConfigNow()

    fun updateDesc(device: String, desc: String)

    fun updateLastConnect(device: Configs.HistoryDevice)

    fun updateLastConnectTagged(device: Configs.HistoryDevice, tagged: Boolean)

    fun removeLastConnect(device: Configs.HistoryDevice)

    fun clearHistoryConnect()

    fun updateScrcpyConfig(config: ScrcpyConfig.CommonConfig)

    fun updateScrcpyConfig(serial: String, config: ScrcpyConfig.Config)

    fun updateSimpleConfig(config: Configs.SimpleConfig)

}