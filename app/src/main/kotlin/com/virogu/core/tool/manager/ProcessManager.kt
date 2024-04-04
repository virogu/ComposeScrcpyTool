package com.virogu.core.tool.manager

import com.virogu.core.device.process.ProcessInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ProcessManager {
    val processListFlow: StateFlow<List<ProcessInfo>>
    val tipsFlow: SharedFlow<String>
    val isBusy: MutableStateFlow<Boolean>

    fun pause()

    fun active()

    fun refresh()

    fun killProcess(info: ProcessInfo)

    fun forceStopProcess(info: ProcessInfo)
}