package com.virogu.tools.process

import com.virogu.bean.ProcessInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface DeviceProcessTool {
    val processListFlow: StateFlow<List<ProcessInfo>>
    val tipsFlow: SharedFlow<String>
    val isBusy: MutableStateFlow<Boolean>

    fun pause()

    fun active()

    fun refresh()

    fun killProcess(pid: String)

    fun forceStopProcess(pid: String)
}