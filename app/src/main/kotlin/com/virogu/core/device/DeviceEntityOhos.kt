package com.virogu.core.device

import com.virogu.core.device.ability.DeviceAbilityAdditional
import com.virogu.core.device.ability.DeviceAbilityFolder
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.ability.DeviceAbilityScrcpy
import com.virogu.core.device.ability.ohos.OhosDeviceAdditionalAbility
import com.virogu.core.device.ability.ohos.OhosDeviceFolderAbility
import com.virogu.core.device.ability.ohos.OhosDeviceProcessAbility
import com.virogu.core.device.ability.ohos.OhosDeviceScrcpyAbility

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:41
 **/
data class DeviceEntityOhos(
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

    override val platform: DevicePlatform = DevicePlatform.OpenHarmony

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
                append("OHOS")
                append("_")
                append(apiVersion)
                append(") ")
            } else {
                append(" ($status) ")
            }
        }
    }

    override val folderAbility: DeviceAbilityFolder = OhosDeviceFolderAbility(this)

    override val processAbility: DeviceAbilityProcess = OhosDeviceProcessAbility(this)

    override val scrcpyAbility: DeviceAbilityScrcpy = OhosDeviceScrcpyAbility(this)

    override val additionalAbility: DeviceAbilityAdditional = OhosDeviceAdditionalAbility(this)

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
    ): DeviceEntityOhos = DeviceEntityOhos(
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