package com.virogu.bean

import kotlinx.serialization.Serializable

/**
 * json sample:
 * {
 *   "user": "root",
 *   "pwd": "123456",
 *   "ppk": [{
 *     "name": "ppk_key.ppk",
 *     "value": "PuTTY-User-Key-File-......",
 *     "pwd": "123456"
 *   }]
 * }
 */
@Serializable
class DeviceSshConfig(
    val user: String = "",
    val pwd: String = "",
    val ppk: List<PPK> = emptyList()
) {
    @Serializable
    data class PPK(
        val name: String,
        val value: String,
        val pwd: String
    )
}