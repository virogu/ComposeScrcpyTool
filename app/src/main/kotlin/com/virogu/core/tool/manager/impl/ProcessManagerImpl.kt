package com.virogu.core.tool.manager.impl

import com.virogu.core.device.Device
import com.virogu.core.device.process.ProcessInfo
import com.virogu.core.tool.connect.DeviceConnect
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.manager.ProcessManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProcessManagerImpl(
    private val initTool: InitTool,
    deviceConnect: DeviceConnect,
) : BaseJobManager(), ProcessManager {
    private var mJob: Job? = null

    @Volatile
    private var active = false

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val selectedOnlineDevice = deviceConnect.currentSelectedDevice.map {
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
                processListFlow.emit(emptyList())
                initJob()
            }.launchIn(scope)
        }
    }

    private fun initJob() {
        mJob?.cancel()
        mJob = scope.launch {
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
        startJob("refresh") {
            val device = currentDevice ?: return@startJob
            refreshProcess(device)
        }
    }

    private suspend fun refreshProcess(device: Device) {
        val process = device.processAbility.refresh()
        processListFlow.emit(process)
    }

    override fun killProcess(info: ProcessInfo) {
        startJob("kill ${info.pid}") {
            val device = currentDevice ?: return@startJob
            device.processAbility.killProcess(info).toast()
            refreshProcess(device)
        }
    }

    override fun forceStopProcess(info: ProcessInfo) {
        startJob("force stop ${info.packageName}") {
            val device = currentDevice ?: return@startJob
            device.processAbility.forceStopProcess(info).toast()
            refreshProcess(device)
        }
    }

    private suspend fun Result<String>.toast() = onSuccess {
        if (it.isNotEmpty()) {
            tipsFlow.emit(it)
        }
    }
}