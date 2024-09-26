package com.virogu.core.config

import com.virogu.core.bean.HistoryDevice
import kotlinx.coroutines.flow.StateFlow

interface HistoryDevicesStore {

    val historyDeviceFlow: StateFlow<List<HistoryDevice>>

    fun updateLastConnect(device: HistoryDevice)

    fun updateLastConnectTagged(device: HistoryDevice, tagged: Boolean)

    fun removeLastConnect(device: HistoryDevice)

    fun clearHistoryConnect()

}