package com.virogu.bean

import kotlinx.serialization.Serializable

val AdbDevice?.isOnline get() = this?.isOnline == true

val AdbDevice?.isOffline get() = this?.isOnline != true

@Serializable
data class AdbDevice(
    val serial: String,
    val model: String,
    val product: String,
    val device: String,
    val status: String,
    val desc: String = model,
) {
    val isOnline = status == "device"

    val showName = buildString {
        if (desc.isNotEmpty()) {
            append(desc)
        } else {
            append(model)
        }
        append("-")
        append(serial)
        if (!isOnline) {
            append(" ($status) ")
        }
    }
}