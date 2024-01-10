package com.virogu.core.tool

import com.virogu.core.bean.DeviceInfo
import kotlinx.coroutines.flow.StateFlow

interface DeviceConnectTool {
    val connectedDevice: StateFlow<List<DeviceInfo>>
    val currentSelectedDevice: StateFlow<DeviceInfo?>
    val isBusy: StateFlow<Boolean>

    fun start()

    fun selectDevice(device: DeviceInfo)

    fun connect(ip: String, port: Int = 5555)

    fun disconnect(device: DeviceInfo)

    fun refresh()

    fun updateCurrentDesc(desc: String)

    fun disconnectAll()

    fun stop()

}