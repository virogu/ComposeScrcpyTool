package com.virogu.core.tool.impl

import com.virogu.core.tool.DeviceConnectTool
import com.virogu.core.tool.SSHTool
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class DeviceConnectToolBase : DeviceConnectTool {

    private val sshTool: SSHTool by DI.global.instance()

    protected suspend fun openDevicePort(ip: String, port: Int): Result<Boolean> {
        logger.info("try open device port, $ip:$port")
        sshTool.connect(ip, SSHVerifyTools.user, SSHVerifyTools.pwd) { session ->
            var r = false
            exec(
                session,
                "setprop service.adb.tcp.port $port",
                "stop adbd",
                "start adbd"
            ).onSuccess {
                r = true
                logger.info("open adb port success")
            }.onFailure { e ->
                logger.info("open adb port fail:\n$e")
            }
            if (!r) {
                exec(
                    session,
                    "param set persist.hdc.mode tcp",
                    "param set persist.hdc.port $port",
                    "hdcd -b"
                ).onSuccess {
                    r = true
                    logger.info("open hdc tcp port success")
                }.onFailure { e ->
                    logger.info("open hdc tcp port fail:\n$e")
                }
            }
            if (!r) {
                throw IllegalStateException("open device port fail")
            }
        }.fold({
            return Result.success(true)
        }, {
            logger.info("$it")
            return Result.failure(it)
        })
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DeviceConnectToolBase::class.java)
    }

}