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
