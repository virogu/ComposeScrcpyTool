/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.config.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.virogu.core.Common
import com.virogu.core.config.*
import java.io.File

class ConfigStoreImpl(name: String = "settings") : ConfigStores {

    private val dataStore = PreferenceDataStoreFactory.create(produceFile = {
        File(Common.projectConfigDir, "${name}.preferences_pb")
    })

    override val deviceDescStore: DeviceDescConfigStore = DescConfigImpl(dataStore)

    override val historyDevicesStore: HistoryDevicesStore = HistoryDeviceConfigImpl(dataStore)

    override val scrcpyConfigStore: ScrcpyConfigStore = ScrcpyConfigImpl(dataStore)

    override val simpleConfigStore: SimpleConfigStore = SimpleConfigImpl(dataStore)

}