package com.virogu.core.tool.manager

import com.virogu.core.bean.Additional
import com.virogu.core.tool.scan.DeviceScan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdditionalManagerImpl(deviceScan: DeviceScan) : AdditionalManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val loadingJobs = mutableMapOf<String, Job>()

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val selectedOnlineDevice = deviceScan.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    override fun exec(additional: Additional) {
        withLock("auxiliary: ${additional.title}") {
            val device = currentDevice ?: return@withLock
            device.additionalAbility.exec(additional)
        }
    }

    private fun withLock(tag: String, block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            isBusy.emit(true)
            mutex.withLock {
                try {
                    block()
                } catch (_: Throwable) {
                } finally {
                }
            }
        }.also { job ->
            addJob(tag, job)
        }
    }

    private fun addJob(tag: String, job: Job) {
        synchronized(loadingJobs) {
            loadingJobs.remove(tag)?.cancel()
            job.invokeOnCompletion {
                runBlocking(Dispatchers.IO) {
                    isBusy.emit(mutex.isLocked)
                }
                removeJob(tag)
            }
            loadingJobs[tag] = job
        }
    }

    private fun removeJob(tag: String) {
        synchronized(loadingJobs) {
            loadingJobs.remove(tag)?.cancel()
        }
    }
}