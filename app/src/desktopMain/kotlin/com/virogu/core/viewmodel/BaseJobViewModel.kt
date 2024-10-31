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
