package com.virogu.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.virogu.core.device.Device
import com.virogu.core.device.process.ProcessInfo
import com.virogu.core.tool.connect.DeviceConnect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-09-11 上午11:37
 **/
class ProcessViewModel : BaseJobViewModel() {
    private val deviceConnect by DI.global.instance<DeviceConnect>()
    private var mJob: Job? = null

    @Volatile
    private var active = false

    private val selectedOnlineDevice = deviceConnect.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    val processListFlow: MutableStateFlow<List<ProcessInfo>> = MutableStateFlow(emptyList())

    val tipsFlow = MutableSharedFlow<String>()

    init {
        start()
    }

    private fun start() {
        selectedOnlineDevice.onEach {
            processListFlow.emit(emptyList())
            initJob()
        }.launchIn(viewModelScope)
    }

    private fun initJob() {
        mJob?.cancel()
        mJob = viewModelScope.launch {
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

    fun pause() {
        active = false
        initJob()
    }

    fun active() {
        active = true
        initJob()
    }

    fun refresh() {
        startJob("refresh") {
            val device = currentDevice ?: return@startJob
            refreshProcess(device)
        }
    }

    private suspend fun refreshProcess(device: Device) {
        val process = device.processAbility.refresh()
        processListFlow.emit(process)
    }

    fun killProcess(info: ProcessInfo) {
        startJob("kill ${info.pid}") {
            val device = currentDevice ?: return@startJob
            device.processAbility.killProcess(info).toast()
            refreshProcess(device)
        }
    }

    fun forceStopProcess(info: ProcessInfo) {
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
