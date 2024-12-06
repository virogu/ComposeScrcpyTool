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

package com.virogu.core.tool.init

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File


interface InitTool {
    val workDir: File
    val resourceDir: File
    val initStateFlow: StateFlow<InitState>

    suspend fun waitStart() = initStateFlow.first {
        it.success
    }

    fun init()

}

sealed class InitState(val success: Boolean = false, val msg: String = "", val subMsg: String = "") {

    data object Default : InitState(false, msg = "正在初始化", subMsg = "请稍等...")

    data object Success : InitState(true)

    data class Error(val error: String, val subError: String) : InitState(false, error, subError)
}