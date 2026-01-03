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

package com.virogu.core.tool

import com.virogu.core.command.AdbCommand
import com.virogu.core.command.BaseCommand
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.PingCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.tool.connect.DeviceConnect
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.ssh.SSHTool
import kotlinx.coroutines.flow.MutableSharedFlow

interface Tools {
    val initTool: InitTool
    val configStores: ConfigStores
    val deviceConnect: DeviceConnect
    val logTool: LogTool
    val notification: MutableSharedFlow<String>

    val sshTool: SSHTool

    val baseCommand: BaseCommand
    val pingCommand: PingCommand
    val adbCommand: AdbCommand
    val hdcCommand: HdcCommand

    fun start()
    fun stop()
}