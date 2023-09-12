package com.virogu.tools.connect

import com.virogu.tools.sshd.SSHTool
import com.virogu.tools.sshd.SSHVerifyTools
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BaseDeviceConnectTool : DeviceConnectTool {

    private val sshTool: SSHTool by DI.global.instance()

    protected suspend fun openDeviceAdb(ip: String): Result<Boolean> {
        sshTool.connect(ip, SSHVerifyTools.user, SSHVerifyTools.pwd) {
            exec(
                it,
                "setprop service.adb.tcp.port 5555",
                "stop adbd",
                "start adbd"
            ).onSuccess {
                logger.info("open device adbd success")
            }.onFailure { e ->
                logger.info("open device adbd fail. $e")
            }
        }.fold({
            return Result.success(true)
        }, {
            return Result.failure(it)
        })
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BaseDeviceConnectTool::class.java)
    }

}