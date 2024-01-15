package com.virogu.core.tool.special.hdc

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.ProcessInfo
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.special.ProcessManage

class ProcessManageHdc(
    private val progressTool: ProgressTool
) : ProcessManage {

    override suspend fun refresh(device: DeviceInfo): List<ProcessInfo> {
        return emptyList()
    }

    override suspend fun killProcess(device: DeviceInfo, info: ProcessInfo): Result<String> {
        return Result.success("")
    }

    override suspend fun forceStopProcess(device: DeviceInfo, info: ProcessInfo): Result<String> {
        return Result.success("")
    }

}