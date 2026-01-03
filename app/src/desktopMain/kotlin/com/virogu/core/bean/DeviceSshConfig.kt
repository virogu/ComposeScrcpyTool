/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.bean

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