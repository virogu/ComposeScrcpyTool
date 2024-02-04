package com.virogu.core.device

import com.virogu.core.device.ability.DeviceAbilityAdditional
import com.virogu.core.device.ability.DeviceAbilityFolder
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.ability.DeviceAbilityScrcpy
import com.virogu.core.device.ability.adb.AndroidDeviceAdditionalAbility
import com.virogu.core.device.ability.adb.AndroidDeviceFolderAbility
import com.virogu.core.device.ability.adb.AndroidDeviceProcessAbility

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
            if (desc.isNotEmpty()) {
                append(desc)
            } else {
                append(model)
            }
            append("-")
            append(serial)
            if (isOnline) {
                append(" (")
                append("A$version")
                append("_")
                append(apiVersion)
                append(") ")
            } else {
                append(" ($status) ")
            }
        }
    }

    override val folderAbility: DeviceAbilityFolder = AndroidDeviceFolderAbility(this)

    override val processAbility: DeviceAbilityProcess = AndroidDeviceProcessAbility(this)

    override val scrcpyAbility: DeviceAbilityScrcpy =
        com.virogu.core.device.ability.adb.AndroidDeviceScrcpyAbility(this)

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