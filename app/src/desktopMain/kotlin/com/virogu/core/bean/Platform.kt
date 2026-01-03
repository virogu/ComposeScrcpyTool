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

sealed class Platform(open val info: String, open val version: String) {
    data class Windows(override val info: String, override val version: String) : Platform(info, version)
    data class Linux(override val info: String, override val version: String) : Platform(info, version)
    data class MacOs(override val info: String, override val version: String) : Platform(info, version)
    data class Unknown(override val info: String, override val version: String) : Platform(info, version)
}