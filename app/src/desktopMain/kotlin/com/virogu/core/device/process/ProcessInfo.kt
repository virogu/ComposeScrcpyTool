/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.device.process

interface ProcessInfo {
    val user: String
    val uid: String
    val pid: String
    val processName: String
    val packageName: String
    val abi: String

    sealed class SortBy(
        val tag: String,
        val sort: (list: Iterable<ProcessInfo>, desc: Boolean) -> List<ProcessInfo>
    ) {
        data object NAME : SortBy("NAME", { list, desc ->
            if (desc) {
                list.sortedByDescending {
                    it.processName
                }
            } else {
                list.sortedBy {
                    it.processName
                }
            }
        })

        data object PID : SortBy("PID", { list, desc ->
            if (desc) {
                list.sortedByDescending {
                    it.pid.toLongOrNull() ?: 0
                }
            } else {
                list.sortedBy {
                    it.pid.toLongOrNull() ?: 0
                }
            }
        })
    }
}