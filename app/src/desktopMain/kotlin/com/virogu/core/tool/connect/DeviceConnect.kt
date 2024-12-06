/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.tool.connect

import com.virogu.core.device.Device
import kotlinx.coroutines.flow.StateFlow

interface DeviceConnect {
    val connectedDevice: StateFlow<List<Device>>
    val currentSelectedDevice: StateFlow<Device?>
    val isBusy: StateFlow<Boolean>

    fun start()

    fun stop()

    fun refresh()

    fun updateCurrentDesc(desc: String)

    fun selectDevice(device: Device)

    fun connect(ip: String, port: Int = 5555)

    fun disconnect(device: Device)

    fun disconnectAll()
}