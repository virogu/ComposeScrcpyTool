package com.virogu.core.tool.impl

import com.virogu.core.bean.Auxiliary
import com.virogu.core.bean.DevicePlatform
import com.virogu.core.bean.ProcessInfo
import com.virogu.core.tool.AuxiliaryTool
import com.virogu.core.tool.DeviceConnectTool
import com.virogu.core.tool.ProgressTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuxiliaryToolImpl(
    deviceConnectTool: DeviceConnectTool,
    private val progressTool: ProgressTool,
) : AuxiliaryTool {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val jobsMutex = Mutex()
    private val loadingJobs = mutableMapOf<String, Job>()

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val selectedOnlineDevice = deviceConnectTool.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    override val processListFlow: MutableStateFlow<List<ProcessInfo>> = MutableStateFlow(emptyList())

    override fun exec(auxiliary: Auxiliary) {
        withLock("auxiliary: ${auxiliary.title}") {
            val device = currentDevice ?: return@withLock
            val array = when (device.platform) {
                DevicePlatform.Android -> arrayOf("adb", "-s", device.serial, *auxiliary.adbCmd)
            }
            progressTool.exec(*array, consoleLog = true)
        }
    }

    private fun withLock(tag: String, block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            mutex.withLock {
                try {
                    block()
                } catch (_: Throwable) {
                } finally {
                }
            }
        }.also { job ->
            jobLock {
                it[tag]?.cancel()
                it[tag] = job
            }
            job.invokeOnCompletion {
                jobLock {
                    it.remove(tag)
                }
            }
        }
    }

    private fun <T> jobLock(block: suspend CoroutineScope.(MutableMap<String, Job>) -> T) = runBlocking {
        jobsMutex.withLock {
            block(loadingJobs).also {
                isBusy.emit(mutex.isLocked)
            }
        }
    }
}