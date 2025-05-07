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

package com.virogu.core.tool

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import com.virogu.core.command.AdbCommand
import com.virogu.core.command.BaseCommand
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.PingCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.config.impl.ConfigStoreImpl
import com.virogu.core.tool.connect.DeviceConnect
import com.virogu.core.tool.connect.DeviceConnectManager
import com.virogu.core.tool.init.*
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.log.LogToolImpl
import com.virogu.core.tool.ssh.SSHTool
import com.virogu.core.tool.ssh.SSHToolImpl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ToolImpl : Tools {
    override val notification = MutableSharedFlow<String>()

    override val configStores: ConfigStores = ConfigStoreImpl()

    override val logTool: LogTool = LogToolImpl()

    override val sshTool: SSHTool = SSHToolImpl()

    override val baseCommand: BaseCommand = BaseCommand()

    override val pingCommand: PingCommand = PingCommand()

    override val adbCommand: AdbCommand = AdbCommand()

    override val hdcCommand: HdcCommand = HdcCommand()

    override val initTool: InitTool by lazy {
        when (Common.platform) {
            is Platform.Windows -> InitToolWindows()
            is Platform.Linux -> InitToolLinux()
            is Platform.MacOs -> InitToolMacOs()
            else -> InitToolDefault()
        }
    }

    override val deviceConnect: DeviceConnect = DeviceConnectManager(initTool, configStores)

    override fun start() {
        logTool.start()
        initTool.init()
        deviceConnect.start()
    }

    override fun stop() {
        deviceConnect.stop()
        runBlocking {
            coroutineScope {
                launch {
                    baseCommand.destroy()
                }
                launch {
                    pingCommand.destroy()
                }
                launch {
                    hdcCommand.destroy()
                }
                launch {
                    adbCommand.destroy()
                }
            }
        }
        logTool.stop()
    }

}