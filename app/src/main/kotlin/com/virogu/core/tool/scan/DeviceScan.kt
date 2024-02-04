package com.virogu.core.tool.scan

import com.virogu.core.device.Device
import kotlinx.coroutines.flow.StateFlow

interface DeviceScan {
    val connectedDevice: StateFlow<List<Device>>
    val currentSelectedDevice: StateFlow<Device?>
    val isBusy: StateFlow<Boolean>

    fun start()

    fun selectDevice(device: Device)

    fun connect(ip: String, port: Int = 5555)

    fun disconnect(device: Device)

    fun refresh()

    fun updateCurrentDesc(desc: String)

    fun disconnectAll()

    fun stop()

}