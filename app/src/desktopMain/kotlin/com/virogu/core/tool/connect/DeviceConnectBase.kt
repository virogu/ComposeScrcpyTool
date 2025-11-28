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

import com.virogu.core.command.PingCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.device.Device
import com.virogu.core.tool.ssh.SSHTool
import com.virogu.core.tool.ssh.SSHVerifyTools
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.apache.sshd.client.session.ClientSession
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

abstract class DeviceConnectBase(
    protected val configStores: ConfigStores,
) : DeviceConnect {
    private val sshTool: SSHTool by DI.global.instance()
    protected val pingCommand: PingCommand by DI.global.instance()

    protected val mutex = Mutex()
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val devices: MutableStateFlow<List<Device>> = MutableStateFlow(emptyList())

    override val connectedDevice: StateFlow<List<Device>> = combine(
        configStores.deviceDescStore.deviceDescFlow,
        devices
    ) { deviceDesc, device ->
        device.map {
            it.copy(desc = deviceDesc[it.serial].orEmpty())
        }
    }.distinctUntilChanged().onEach(::onDeviceConnectedChanged).stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val currentSelectedDevice: MutableStateFlow<Device?> = MutableStateFlow(null)

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    protected open suspend fun afterStarted() {

    }

    abstract suspend fun doConnect(ip: String, port: Int): Boolean

    abstract suspend fun doDisConnect(device: Device)

    abstract suspend fun doDisConnectAll()

    abstract suspend fun doOpenTcpPort(ssh: SSHTool, session: ClientSession, port: Int): Boolean

    abstract suspend fun refreshDevice(showLog: Boolean = false): List<Device>

    protected suspend fun openTcpPort(ip: String, port: Int): Boolean {
        logger.info { "try open device port, $ip:$port" }
        return sshTool.connect(ip, SSHVerifyTools.user, SSHVerifyTools.pwd) { session ->
            val r = doOpenTcpPort(this, session, port)
            if (!r) {
                throw IllegalStateException("open device [$ip] port [$port] fail")
            }
        }.onFailure {
            logger.warn { it.localizedMessage }
        }.isSuccess
    }

    private suspend fun onDeviceConnectedChanged(list: List<Device>) {
        val current = currentSelectedDevice.value
        if (current == null) {
            list.firstOrNull {
                it.isOnline
            }?.also {
                currentSelectedDevice.emit(it)
            }
        } else {
            //已选择的设备已经断开了的话自动选择第一个已连接的设备
            val find = list.find {
                it.serial == current.serial
            }
            if (find == null) {
                currentSelectedDevice.emit(list.firstOrNull { it.isOnline })
            } else if (find != current) {
                currentSelectedDevice.emit(find)
            }
        }
    }

    protected fun withLock(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            mutex.lock()
            isBusy.emit(true)
            try {
                block()
            } catch (_: Throwable) {
            } finally {
                isBusy.emit(false)
                mutex.unlock()
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

}