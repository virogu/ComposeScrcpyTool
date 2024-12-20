/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.tool.connect

import com.virogu.core.command.AdbCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.device.Device
import com.virogu.core.device.DeviceEntityAndroid
import com.virogu.core.device.DevicePlatform
import com.virogu.core.tool.ssh.SSHTool
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.sshd.client.session.ClientSession
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.util.regex.Pattern

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:59
 **/
abstract class DeviceConnectAdb(configStores: ConfigStores) : DeviceConnectBase(configStores) {
    private val cmd: AdbCommand by DI.global.instance()
    protected val adbCmd get() = cmd

    override suspend fun doConnect(ip: String, port: Int): Boolean {
        cmd.adb("disconnect", "${ip}:${port}", consoleLog = true)
        logger.info { "start adb connect" }
        val r = cmd.adb("connect", "${ip}:${port}", timeout = 3L, consoleLog = true).getOrNull()?.takeIf {
            it.isNotEmpty()
        } ?: run {
            return false
        }
        logger.info { r }
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
            logger.info { "open adb port [$port] success" }
        }.onFailure {
            logger.info { "open adb port [$port] fail: ${it.localizedMessage}" }
        }
        return r
    }

    override suspend fun refreshDevice(showLog: Boolean): List<Device> = try {
        val process = cmd.adb("devices", "-l", showLog = false, consoleLog = showLog).getOrThrow()
        val result = process.split("\n")
        result.mapNotNull { line ->
            //127.0.0.1:58526        device product:windows_x86_64 model:Subsystem_for_Android_TM_ device:windows_x86_64 transport_id:5
            //emulator-5556 device product:google_x86_64 model:Android_x86_64 device:generic_x86_64
            //emulator-5554 device product:google_x86 model:Android_x86 device:generic_x86  transport_id:5
            //0a388e93      device usb:1-1 product:razor model:Nexus_7 device:flo
            val matcher = Pattern.compile(
                "^(\\S+)\\s+(\\S+)\\s+(?:usb:\\S+\\s+)?product:(\\S+)\\s+model:(\\S+)\\s+device:(\\S+)(?:\\s+transport_id:)?(\\S+)?(.*)$"
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

                val apiVersion = isOnline.takeIf {
                    it
                }?.let {
                    adbGetProp(serial, ANDROID_API_VERSION)
                } ?: ""

                val androidVersion = isOnline.takeIf {
                    it
                }?.let {
                    adbGetProp(serial, ANDROID_RELEASE_VERSION)
                } ?: ""

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
        //e.printStackTrace()
        emptyList()
    }

    suspend fun adbDisConnectAll() {
        cmd.adb("disconnect", showLog = true)
    }

    private suspend fun adbGetProp(serial: String, prop: String, default: String = ""): String {
        return cmd.adb(
            "-s", serial, "shell", "getprop", prop,
            consoleLog = false, showLog = false
        ).getOrNull() ?: default
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        private const val ANDROID_API_VERSION = "ro.build.version.sdk"
        private const val ANDROID_RELEASE_VERSION = "ro.build.version.release"

    }
}