package com.virogu.core.config

interface ConfigStores {

    val deviceDescStore: DeviceDescConfigStore

    val historyDevicesStore: HistoryDevicesStore

    val scrcpyConfigStore: ScrcpyConfigStore

    val simpleConfigStore: SimpleConfigStore

}