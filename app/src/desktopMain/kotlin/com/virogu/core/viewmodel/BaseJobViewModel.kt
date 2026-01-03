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

package com.virogu.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * @author Virogu
 * @since 2024-09-11 上午10:54
 **/
open class BaseJobViewModel : ViewModel() {
    val isBusy: StateFlow<Boolean> get() = mIsBusy
    protected val mutex = Mutex()
    protected open val mIsBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    protected fun startLineJob(tag: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            lineJob(tag, block)
        }
    }

    protected suspend fun <T> lineJob(tag: String? = null, block: suspend () -> T): T {
        tag?.also {
            println("job [$it] wait.")
        }
        mutex.lock()
        tag?.also {
            println("job [$it] exec.")
        }
        mIsBusy.value = true
        try {
            return block()
        } finally {
            tag?.also {
                println("job [$it] finish.")
            }
            mIsBusy.value = false
            mutex.unlock()
        }
    }

}
