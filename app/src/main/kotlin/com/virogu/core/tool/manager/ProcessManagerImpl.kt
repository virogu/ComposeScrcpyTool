package com.virogu.core.tool.manager

import com.virogu.core.bean.ProcessInfo
import com.virogu.core.device.Device
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.scan.DeviceScan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProcessManagerImpl(
    private val initTool: InitTool,
    deviceScan: DeviceScan,
) : ProcessManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val loadingJobs = mutableMapOf<String, Job>()
    private var mJob: Job? = null

    @Volatile
    private var active = false

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val selectedOnlineDevice = deviceScan.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    override val processListFlow: MutableStateFlow<List<ProcessInfo>> = MutableStateFlow(emptyList())

    override val tipsFlow = MutableSharedFlow<String>()

    init {
        start()
    }

    private fun start() {
        scope.launch {
            initTool.waitStart()
            selectedOnlineDevice.onEach {
                initJob()
            }.launchIn(scope)
        }
    }

    private fun initJob() {
        mJob?.cancel()
        mJob = scope.launch {
            processListFlow.emit(emptyList())
            if (!active) {
                return@launch
            }
            while (isActive) {
                val device = currentDevice
                if (active && device != null) {
                    refreshProcess(device)
                }
                delay(5000)
            }
        }
    }

    override fun pause() {
        active = false
        initJob()
    }

    override fun active() {
        active = true
        initJob()
    }

    override fun refresh() {
        withLock("refresh") {
            val device = currentDevice ?: return@withLock
            refreshProcess(device)
        }
    }

    private suspend fun refreshProcess(device: Device) {
        val process = device.processAbility.refresh()
        delay(10)
        processListFlow.emit(process)
    }

    override fun killProcess(info: ProcessInfo) {
        withLock("kill ${info.pid}") {
            val device = currentDevice ?: return@withLock
            device.processAbility.killProcess(info).toast()
            refreshProcess(device)
        }
    }

    override fun forceStopProcess(info: ProcessInfo) {
        withLock("force stop ${info.packageName}") {
            val device = currentDevice ?: return@withLock
            device.processAbility.forceStopProcess(info).toast()
            refreshProcess(device)
        }
    }

    private suspend fun Result<String>.toast() = onSuccess {
        if (it.isNotEmpty()) {
            tipsFlow.emit(it)
        }
    }

    private fun withLock(tag: String, block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            isBusy.emit(true)
            mutex.withLock {
                try {
                    block()
                } catch (_: Throwable) {
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