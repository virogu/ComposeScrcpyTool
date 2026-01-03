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

package com.virogu.core.device

import com.virogu.core.device.ability.DeviceAbilityAdditional
import com.virogu.core.device.ability.DeviceAbilityFolder
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.ability.DeviceAbilityScrcpy

enum class DevicePlatform(val platform: String) {
    Android("Android"),
    OpenHarmony("OpenHarmony")
}

/**
 * @author Virogu
 * @since 2024-03-27 下午 4:54
 **/
sealed class Device(
    open val serial: String,
    open val model: String,
    open val product: String,
    open val device: String,
    open val desc: String,
    open val status: String,
    open val version: String,
    open val apiVersion: String,
    open val isOnline: Boolean,
) {
    val isOffline get() = !isOnline
    abstract val platform: DevicePlatform
    abstract val showName: String
    abstract val folderAbility: DeviceAbilityFolder
    abstract val processAbility: DeviceAbilityProcess
    abstract val scrcpyAbility: DeviceAbilityScrcpy
    abstract val additionalAbility: DeviceAbilityAdditional

    abstract fun copy(
        serial: String = this.serial,
        model: String = this.model,
        product: String = this.product,
        device: String = this.device,
        desc: String = this.desc,
        status: String = this.status,
        version: String = this.version,
        apiVersion: String = this.apiVersion,
        showName: String = this.showName,
        isOnline: Boolean = this.isOnline,
        platform: DevicePlatform = this.platform,
    ): Device
}
