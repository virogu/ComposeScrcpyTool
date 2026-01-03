/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.di

import com.virogu.core.command.AdbCommand
import com.virogu.core.command.BaseCommand
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.PingCommand
import com.virogu.core.config.*
import com.virogu.core.tool.ToolImpl
import com.virogu.core.tool.Tools
import com.virogu.core.tool.connect.DeviceConnect
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.ssh.SSHTool
import kotlinx.coroutines.flow.MutableSharedFlow
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.conf.global

@Volatile
private var init = false

@Synchronized
fun initDi(
    onBind: DI.MainBuilder.() -> Unit = {}
) {
    if (init) {
        return
    }
    init = true
    DI.global.addConfig {
        val tools = ToolImpl()
        bindSingleton<MutableSharedFlow<String>>("notification") {
            tools.notification
        }
        val configStores = tools.configStores
        bindSingleton<Tools> {
            tools
        }
        bindSingleton<InitTool> {
            tools.initTool
        }
        bindSingleton<BaseCommand> {
            tools.baseCommand
        }
        bindSingleton<PingCommand> {
            tools.pingCommand
        }
        bindSingleton<AdbCommand> {
            tools.adbCommand
        }
        bindSingleton<HdcCommand> {
            tools.hdcCommand
        }
        bindSingleton<ConfigStores> {
            configStores
        }
        bindSingleton<HistoryDevicesStore> {
            configStores.historyDevicesStore
        }
        bindSingleton<DeviceDescConfigStore> {
            configStores.deviceDescStore
        }
        bindSingleton<ScrcpyConfigStore> {
            configStores.scrcpyConfigStore
        }
        bindSingleton<SimpleConfigStore> {
            configStores.simpleConfigStore
        }
        bindSingleton<SSHTool> {
            tools.sshTool
        }
        bindSingleton<DeviceConnect> {
            tools.deviceConnect
        }
        bindSingleton<LogTool> {
            tools.logTool
        }
        onBind()
    }
}