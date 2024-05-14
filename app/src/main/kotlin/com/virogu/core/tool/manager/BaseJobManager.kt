package com.virogu.core.tool.manager

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @author Virogu
 * @since 2024-04-07 ÏÂÎç5:28
 **/
abstract class BaseJobManager {
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected open val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
            isBusy.emit(false)
        }
    }

    protected fun startJob(tag: String, block: suspend CoroutineScope.() -> Unit) {
        synchronized(activeJobs) {
            activeJobs.remove(tag)?.cancel()
            val job = scope.launch {
                isBusy.emit(true)
                try {
                    block()
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    isBusy.emit(false)
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