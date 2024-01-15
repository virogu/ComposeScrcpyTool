package com.virogu.core.tool.special

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.ProcessInfo

interface ProcessManage {
    suspend fun refresh(device: DeviceInfo): List<ProcessInfo>

    suspend fun killProcess(device: DeviceInfo, info: ProcessInfo): Result<String>

    suspend fun forceStopProcess(device: DeviceInfo, info: ProcessInfo): Result<String>
}