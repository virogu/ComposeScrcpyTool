package com.virogu.tools.config

interface ConfigStores {

    val deviceDescStore: DeviceDescConfigStore

    val historyDevicesStore: HistoryDevicesStore

    val scrcpyConfigStore: ScrcpyConfigStore

    val simpleConfigStore: SimpleConfigStore

}