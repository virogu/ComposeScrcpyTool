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
import com.virogu.core.device.ability.adb.AndroidDeviceAdditionalAbility
import com.virogu.core.device.ability.adb.AndroidDeviceFolderAbility
import com.virogu.core.device.ability.adb.AndroidDeviceProcessAbility
import com.virogu.core.device.ability.adb.AndroidDeviceScrcpyAbility

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:41
 **/
data class DeviceEntityAndroid(
    override val serial: String,
    override val model: String,
    override val product: String,
    override val device: String,
    override val desc: String,
    override val status: String,
    override val version: String,
    override val apiVersion: String,
    override val isOnline: Boolean,
) : Device(serial, model, product, device, desc, status, version, apiVersion, isOnline) {
    override val platform: DevicePlatform = DevicePlatform.Android

    override val showName: String by lazy {
        buildString {
            append(desc.ifEmpty { model }.ifEmpty { "Unknown" })
            append("-")
            append(serial)
            if (isOnline) {
                append(" (")
                if (version.isNotEmpty()) {
                    append("A$version")
                } else {
                    append("Unknown")
                }
                append("_")
                append(apiVersion.ifEmpty { "Unknown" })
                append(") ")
            } else {
                append(" ($status) ")
            }
        }
    }

    override val folderAbility: DeviceAbilityFolder = AndroidDeviceFolderAbility(this)
    override val processAbility: DeviceAbilityProcess = AndroidDeviceProcessAbility(this)
    override val scrcpyAbility: DeviceAbilityScrcpy = AndroidDeviceScrcpyAbility(this)
    override val additionalAbility: DeviceAbilityAdditional = AndroidDeviceAdditionalAbility(this)

    override fun copy(
        serial: String,
        model: String,
        product: String,
        device: String,
        desc: String,
        status: String,
        version: String,
        apiVersion: String,
        showName: String,
        isOnline: Boolean,
        platform: DevicePlatform
    ): DeviceEntityAndroid = DeviceEntityAndroid(
        serial = serial,
        model = model,
        product = product,
        device = device,
        desc = desc,
        status = status,
        version = version,
        apiVersion = apiVersion,
        isOnline = isOnline
    )

}