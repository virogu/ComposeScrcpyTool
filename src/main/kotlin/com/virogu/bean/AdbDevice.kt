package com.virogu.bean

import kotlinx.serialization.Serializable

@Serializable
data class AdbDevice(
    val serial: String,
    val isOnline: Boolean,
    val desc: String = "Phone",
) {
    val showName
        get() = if (isOnline) {
            "${desc}-${serial}"
        } else {
            "${desc}-${serial} (离线)"
        }
}