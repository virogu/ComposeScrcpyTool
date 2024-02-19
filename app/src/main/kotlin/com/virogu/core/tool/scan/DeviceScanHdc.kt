package com.virogu.core.tool.scan

import com.virogu.core.command.HdcCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.device.Device
import com.virogu.core.device.DeviceEntityOhos
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
abstract class DeviceScanHdc(configStores: ConfigStores) : DeviceScanAdb(configStores) {
    private val cmd: HdcCommand by DI.global.instance()
    protected val hdcCmd get() = cmd

    override suspend fun doConnect(ip: String, port: Int): Boolean {
        val success = super.doConnect(ip, port)
        if (success) {
            return true
        }
        logger.info("try hdc connect")
        cmd.hdc("tconn", "${ip}:${port}", "-remove", consoleLog = true)
        val r = cmd.hdc(
            "tconn", "${ip}:${port}", timeout = 3L, consoleLog = true
        ).getOrNull()?.takeIf {
            it.isNotEmpty()
        } ?: run {
            return false
        }
        logger.info(r)
        val failed = r.contains("failed", true)
        return !failed
    }

    override suspend fun refreshDevice(): List<Device> {
        val list1 = super.refreshDevice()
        val list2 = try {
            val process = cmd.hdc("list", "targets", "-v", timeout = 2, consoleLog = false).getOrThrow()
            val result = process.split("\n")
            result.mapNotNull { line ->
                //192.168.5.128:10178   TCP     Offline                 hdc
                //192.168.5.128:5555    TCP     Offline     localhost   hdc
                //192.168.5.131:5555    TCP     Connected   localhost   hdc
                //192.168.5.255:5555    TCP     Offline                 hdc
                //COM1                  UART    Ready                   hdc
                val matcher = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+)(.*)$").matcher(line.trim())
                if (!matcher.find()) {
                    return@mapNotNull null
                } else {
                    val serial = matcher.group(1) ?: return@mapNotNull null
                    //val type = matcher.group(2) ?: return@mapNotNull null
                    val status = matcher.group(3) ?: return@mapNotNull null
                    val isOnline = status.equals("Connected", ignoreCase = true)
                    if (!isOnline) {
                        return@mapNotNull null
                    }
                    val apiVersion = hdcGetProp(serial, OHOS_API_VERSION)
                    val releaseName = hdcGetProp(serial, OHOS_FULL_NAME)
                    val product = hdcGetProp(serial, OHOS_PRODUCT_NAME)
                    val model = hdcGetProp(serial, OHOS_MODEL_NAME)
                    DeviceEntityOhos(
                        serial = serial,
                        status = status,
                        product = product,
                        model = model,
                        apiVersion = apiVersion,
                        version = releaseName,
                        device = product,
                        desc = model,
                        isOnline = true,
                    )
                }
            }.sortedByDescending {
                it.isOnline
            }
        } catch (e: Throwable) {
            //e.printStackTrace()
            emptyList()
        }
        return list1 + list2
    }

    override suspend fun doDisConnect(device: Device) {
        super.doDisConnect(device)
        if (device.platform != DevicePlatform.OpenHarmony) {
            return
        }
        cmd.hdc("tconn", device.serial, "-remove", showLog = true)
    }

    override suspend fun doDisConnectAll() {
        super.doDisConnectAll()
        connectedDevice.value.filter {
            it.platform == DevicePlatform.OpenHarmony
        }.forEach {
            cmd.hdc("tconn", it.serial, "-remove", showLog = true)
        }
    }

    override suspend fun doOpenTcpPort(ssh: SSHTool, session: ClientSession, port: Int): Boolean {
        val r = super.doOpenTcpPort(ssh, session, port)
        if (r) {
            return true
        }
        ssh.exec(
            session, "param set persist.hdc.mode all",
            "param set persist.hdc.port $port",
            //"hdcd -b"
        ).onSuccess {
            logger.info("open hdc tcp port success")
            logger.info("需要重启设备，请重启设备后重新连接")
            ssh.exec(session, "reboot")
        }.onFailure { e ->
            logger.info("open hdc tcp port fail:\n$e")
        }
        return false
    }

    private suspend fun hdcGetProp(serial: String, prop: String, default: String = "Unknown"): String {
        return cmd.hdc(
            "-t", serial, "shell",
            "param", "get", prop, timeout = 1, consoleLog = false
        ).getOrNull()?.takeUnless {
            it.isEmpty() || it.contains("fail", ignoreCase = true)
        } ?: default
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val OHOS_API_VERSION = "const.ohos.apiversion"
        private const val OHOS_FULL_NAME = "const.ohos.fullname"
        private const val OHOS_PRODUCT_NAME = "const.product.name"
        private const val OHOS_MODEL_NAME = "const.product.model"
    }
}