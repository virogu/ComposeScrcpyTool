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

import com.virogu.core.config.ConfigStores
import com.virogu.core.device.Device
import com.virogu.core.tool.init.InitTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock

class DeviceConnectManager(
    private val initTool: InitTool,
    configStores: ConfigStores,
) : DeviceConnectHdc(configStores) {
    private val autoRefresh = configStores.simpleConfigStore.simpleConfig.map {
        it.autoRefresh
    }.stateIn(scope, SharingStarted.Eagerly, true)

    private var mJob: Job? = null
    private var refreshJob: Job? = null

    override fun start() {
        mJob?.cancel()
        mJob = scope.launch {
            logger.info { "等待程序初始化" }
            initTool.waitStart()
            logger.info { "初始化成功" }
            delay(1000)
            afterStarted()
            delay(1000)
            autoRefresh.onEach(::autoRefreshChanged).launchIn(this)
        }
    }

    private suspend fun autoRefreshChanged(enable: Boolean) {
        logger.info { "auto refresh: $enable" }
        refreshJob?.cancel()
        if (!enable) {
            return
        }
        refreshJob = scope.launch {
            while (isActive) {
                if (!autoRefresh.value) {
                    return@launch
                }
                autoRefresh()
                delay(10_000)
            }
        }
    }

    override fun selectDevice(device: Device) {
        currentSelectedDevice.tryEmit(device)
    }

    override fun connect(ip: String, port: Int) {
        withLock {
            logger.info { "正在连接 [$ip:$port]" }
            val ping = pingCommand.ping(ip)
            if (!ping) {
                logger.warn { "无法访问 [$ip], 请检查设备是否在线" }
                return@withLock
            }
            var r = doConnect(ip, port)
            if (!r) {
                r = openTcpPort(ip, port)
                if (r) {
                    logger.info { "重新连接 [$ip:$port]" }
                    doConnect(ip, port)
                } else {
                    logger.warn { "open device [$ip] port [$port] fail" }
                }
            }
            innerRefreshDevices(true)
        }
    }

    override fun disconnect(device: Device) {
        withLock {
            doDisConnect(device)
            innerRefreshDevices(true)
        }
    }

    override fun refresh() {
        withLock {
            innerRefreshDevices(true)
        }
    }

    override fun disconnectAll() {
        withLock {
            doDisConnectAll()
            innerRefreshDevices(true)
        }
    }

    private suspend fun autoRefresh() {
        if (!autoRefresh.value) {
            return
        }
        if (!initTool.initStateFlow.value.success) {
            return
        }
        mutex.withLock {
            innerRefreshDevices()
        }
    }

    private suspend fun innerRefreshDevices(showLog: Boolean = false) {
        val list = refreshDevice(showLog)
        devices.emit(list)
    }

    override fun updateCurrentDesc(desc: String) {
        withLock {
            val current = currentSelectedDevice.value ?: return@withLock
            if (desc == current.desc) {
                return@withLock
            }
            val new = current.copy(desc = desc)
            configStores.deviceDescStore.updateDesc(current.serial, desc)
            currentSelectedDevice.emit(new)
        }
    }

    override fun stop() {
        mJob?.cancel()
        scope.cancel()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
