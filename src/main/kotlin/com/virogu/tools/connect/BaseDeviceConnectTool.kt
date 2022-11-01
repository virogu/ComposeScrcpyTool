package com.virogu.tools.connect

abstract class BaseDeviceConnectTool : DeviceConnectTool {

    protected suspend fun openDeviceAdb(ip: String): Result<Boolean> {
        return Result.success(true)
    }

}