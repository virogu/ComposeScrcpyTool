package com.virogu.bean

import kotlinx.serialization.Serializable

@Serializable
data class AdbDevice(
    val serial: String,
    val desc: String = "Phone",
) {
    val showName get() = "${desc}-${serial}"
}