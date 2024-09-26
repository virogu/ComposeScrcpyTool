package com.virogu.core.bean

sealed class Platform(open val info: String, open val version: String) {
    data class Windows(override val info: String, override val version: String) : Platform(info, version)
    data class Linux(override val info: String, override val version: String) : Platform(info, version)
    data class MacOs(override val info: String, override val version: String) : Platform(info, version)
    data class Unknown(override val info: String, override val version: String) : Platform(info, version)
}