package com.virogu.tools.connect

import com.virogu.bean.AdbDevice
import kotlinx.coroutines.flow.StateFlow

interface DeviceConnectTool {
    val connectedDevice: StateFlow<List<AdbDevice>>
    val currentSelectedDevice: StateFlow<AdbDevice?>
    val isBusy: StateFlow<Boolean>

    fun start()

    fun selectDevice(device: AdbDevice)

    fun connect(ip: String, port: Int = 5555)

    fun disconnect(device: AdbDevice)

    fun refresh()

    fun updateCurrentDesc(desc: String)

    fun disconnectAll()

    fun stop()

}