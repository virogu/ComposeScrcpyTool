package com.virogu.bean

import kotlinx.serialization.Serializable

@Serializable
data class Configs(
    val value: Map<String, String> = emptyMap()
) {

    @Serializable
    data class HistoryDevice(
        val timeMs: Long,
        val ip: String,
        val port: Int = 5555,
        val tagged: Boolean = false
    ) {
        val showName: String get() = "$ip:$port"
    }

    @Serializable
    data class ScrcpyConfig(
        val keepInTop: Boolean = false,
        val enableAudio: Boolean = false,
    )
}