package com.virogu.core.tool.scan

import com.virogu.core.command.AdbCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.device.Device
import com.virogu.core.device.DeviceEntityAndroid
import com.virogu.core.device.DevicePlatform
import com.virogu.core.tool.ssh.SSHTool
import org.apache.sshd.client.session.ClientSession
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:59
 **/
abstract class DeviceScanAdb(configStores: ConfigStores) : DeviceScanBase(configStores) {
    private val cmd: AdbCommand by DI.global.instance()
    protected val adbCmd get() = cmd

    override suspend fun doConnect(ip: String, port: Int): Boolean {
        cmd.adb("disconnect", "${ip}:${port}")
        logger.info("try adb connect")
        val r = cmd.adb("connect", "${ip}:${port}", timeout = 3L, consoleLog = true).getOrNull()?.takeIf {
            it.isNotEmpty()
        } ?: run {
            return false
        }
        logger.info(r)
        // cannot connect to 192.168: 由于目标计算机积极拒绝，无法连接。 (10061)
        // 尝试通过SSH连接到设备再打开ADB
        val failed = r.contains("cannot connect to", true) ||
                r.contains("unable to connect", true) ||
                r.contains("failed to connect", true) ||
                r.contains("connection refused", true)
        return !failed
    }

    override suspend fun doDisConnect(device: Device) {
        if (device.platform != DevicePlatform.Android) {
            return
        }
        cmd.adb("disconnect", device.serial)

    }

    override suspend fun doDisConnectAll() {
        cmd.adb("disconnect", showLog = true)
    }

    override suspend fun doOpenTcpPort(ssh: SSHTool, session: ClientSession, port: Int): Boolean {
        var r = false
        ssh.exec(
            session, "setprop service.adb.tcp.port $port",
            "stop adbd",
            "start adbd"
        ).onSuccess {
            r = true
            logger.info("open adb port success")
        }.onFailure { e ->
            logger.info("open adb port fail:\n$e")
        }
        return r
    }

    override suspend fun refreshDevice(): List<Device> = try {
        val process = cmd.adb("devices", "-l").getOrThrow()
        val result = process.split("\n")
        result.mapNotNull { line ->
            //127.0.0.1:58526        device product:windows_x86_64 model:Subsystem_for_Android_TM_ device:windows_x86_64 transport_id:5
            val matcher = Pattern.compile(
                "^(\\S+)\\s+(\\S+)\\s+product:(\\S+)\\s+model:(\\S+)\\s+device:(\\S+)\\s+(transport_id:)?(\\S+)?(.*)$"
            ).matcher(line.trim())
            if (!matcher.find()) {
                return@mapNotNull null
            } else {
                val serial = matcher.group(1) ?: return@mapNotNull null
                val status = matcher.group(2) ?: return@mapNotNull null
                val product = matcher.group(3) ?: return@mapNotNull null
                val model = matcher.group(4) ?: return@mapNotNull null
                val device = matcher.group(5) ?: return@mapNotNull null
                val isOnline = status == "device"
                val apiVersion = if (isOnline) {
                    adbGetProp(serial, ANDROID_API_VERSION)
                } else {
                    " Unknown"
                }
                val androidVersion = if (isOnline) {
                    adbGetProp(serial, ANDROID_RELEASE_VERSION)
                } else {
                    " Unknown"
                }
                DeviceEntityAndroid(
                    serial = serial,
                    status = status,
                    product = product,
                    model = model,
                    version = androidVersion,
                    apiVersion = apiVersion,
                    device = device,
                    desc = model,
                    isOnline = isOnline
                )
            }
        }.sortedByDescending {
            it.isOnline
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        emptyList()
    }

    suspend fun adbDisConnectAll() {
        cmd.adb("disconnect", showLog = true)
    }

    private suspend fun adbGetProp(serial: String, prop: String, default: String = ""): String {
        return cmd.adb("-s", serial, "shell", "getprop", prop).getOrNull() ?: default
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        private const val ANDROID_API_VERSION = "ro.build.version.sdk"
        private const val ANDROID_RELEASE_VERSION = "ro.build.version.release"

    }
}