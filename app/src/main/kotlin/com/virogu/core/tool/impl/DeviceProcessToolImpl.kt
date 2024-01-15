package com.virogu.core.tool.impl

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.DevicePlatform
import com.virogu.core.bean.ProcessInfo
import com.virogu.core.init.InitTool
import com.virogu.core.tool.DeviceConnectTool
import com.virogu.core.tool.DeviceProcessTool
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.special.ProcessManage
import com.virogu.core.tool.special.adb.ProcessManageAdb
import com.virogu.core.tool.special.hdc.ProcessManageHdc
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeviceProcessToolImpl(
    private val initTool: InitTool,
    deviceConnectTool: DeviceConnectTool,
    private val progressTool: ProgressTool,
) : DeviceProcessTool {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val jobsMutex = Mutex()
    private val loadingJobs = mutableMapOf<String, Job>()
    private var mJob: Job? = null

    private val processTools = mapOf(
        DevicePlatform.Android to ProcessManageAdb(progressTool),
        DevicePlatform.OpenHarmony to ProcessManageHdc(progressTool),
    )

    private val DeviceInfo.processManage: ProcessManage get() = processTools[platform]!!


    @Volatile
    private var active = false

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val selectedOnlineDevice = deviceConnectTool.currentSelectedDevice.map {
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
            initTool.initStateFlow.first {
                it.success
            }
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

    private suspend fun refreshProcess(device: DeviceInfo) {
        val process = device.processManage.refresh(device)
        delay(10)
        processListFlow.emit(process)
    }

    override fun killProcess(info: ProcessInfo) {
        withLock("kill ${info.pid}") {
            val device = currentDevice ?: return@withLock
            device.processManage.killProcess(device, info).toast()
            refreshProcess(device)
        }
    }

    override fun forceStopProcess(info: ProcessInfo) {
        withLock("force stop ${info.packageName}") {
            val device = currentDevice ?: return@withLock
            device.processManage.forceStopProcess(device, info).toast()
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