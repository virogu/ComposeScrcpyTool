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

package com.virogu.core.device.ability

import com.virogu.core.device.process.ProcessInfo

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:35
 **/
interface DeviceAbilityProcess {
    suspend fun refresh(): List<ProcessInfo>

    suspend fun killProcess(info: ProcessInfo): Result<String>

    suspend fun forceStopProcess(info: ProcessInfo): Result<String>
}