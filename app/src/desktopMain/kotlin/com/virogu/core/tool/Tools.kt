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