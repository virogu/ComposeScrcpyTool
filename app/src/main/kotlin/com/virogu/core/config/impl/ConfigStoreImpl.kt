package com.virogu.core.config.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.virogu.core.config.*
import com.virogu.core.projectDataDir
import java.io.File

class ConfigStoreImpl(name: String = "settings") : ConfigStores {

    private val dataStore = PreferenceDataStoreFactory.create(produceFile = {
        File(projectDataDir, "config/${name}.preferences_pb")
    })

    override val deviceDescStore: DeviceDescConfigStore = DescConfigImpl(dataStore)

    override val historyDevicesStore: HistoryDevicesStore = HistoryDeviceConfigImpl(dataStore)

    override val scrcpyConfigStore: ScrcpyConfigStore = ScrcpyConfigImpl(dataStore)

    override val simpleConfigStore: SimpleConfigStore = SimpleConfigImpl(dataStore)

}