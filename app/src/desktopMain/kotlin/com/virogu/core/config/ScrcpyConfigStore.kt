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

package com.virogu.core.config

import com.virogu.core.bean.ScrcpyConfig
import kotlinx.coroutines.flow.StateFlow

interface ScrcpyConfigStore {

    val scrcpyConfigFlow: StateFlow<ScrcpyConfig>

    fun updateScrcpyConfig(config: ScrcpyConfig.CommonConfig)

    fun updateScrcpyConfig(serial: String, config: ScrcpyConfig.Config)

    fun removeScrcpyConfig(serial: String)

    fun clearConfig()

}