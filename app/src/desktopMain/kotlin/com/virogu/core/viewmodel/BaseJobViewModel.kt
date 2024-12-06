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

package com.virogu.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * @author Virogu
 * @since 2024-09-11 上午10:54
 **/
open class BaseJobViewModel : ViewModel() {
    val isBusy: StateFlow<Boolean> get() = mIsBusy
    protected open val mIsBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val activeJobs = mutableMapOf<String, Job>()

    protected fun getJob(tag: String): Job? {
        synchronized(activeJobs) {
            return activeJobs[tag]
        }
    }

    protected fun cancelAllJob() {
        synchronized(activeJobs) {
            activeJobs.forEach { (_, v) ->
                v.cancel()
            }
            activeJobs.clear()
        }
        runBlocking(Dispatchers.IO) {
            mIsBusy.emit(false)
        }
    }

    protected fun startJob(tag: String, block: suspend CoroutineScope.() -> Unit) {
        synchronized(activeJobs) {
            activeJobs.remove(tag)?.cancel()
            val job = viewModelScope.launch {
                mIsBusy.emit(true)
                try {
                    block()
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    mIsBusy.emit(false)
                }
            }
            if (!job.isActive) {
                return@synchronized
            }
            activeJobs[tag] = job
            job.invokeOnCompletion {
                synchronized(activeJobs) {
                    activeJobs.remove(tag)
                }
            }
        }
    }

}
