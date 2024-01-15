package com.virogu.core.bean

import com.virogu.core.bean.DevicePlatform.Android
import com.virogu.core.bean.DevicePlatform.OpenHarmony
import kotlinx.serialization.Serializable

val DeviceInfo?.isOnline get() = this?.isOnline == true

val DeviceInfo?.isOffline get() = this?.isOnline != true

enum class DevicePlatform(val platform: String) {
    Android("Android"),
    OpenHarmony("OpenHarmony")
}

@Serializable
data class DeviceInfo(
    val platform: DevicePlatform,
    val serial: String,
    val model: String,
    val product: String,
    val device: String,
    val status: String,
    val version: String,
    val apiVersion: String,
    val desc: String = model,
    val isOnline: Boolean,
) {
    val showName = buildString {
        if (desc.isNotEmpty()) {
            append(desc)
        } else {
            append(model)
        }
        append("-")
        append(serial)
        if (isOnline) {
            append(" (")
            when (platform) {
                Android -> append("A$version")
                OpenHarmony -> append("OHOS")
            }
            append("_")
            append(apiVersion)
            append(") ")
        } else {
            append(" ($status) ")
        }
    }
}