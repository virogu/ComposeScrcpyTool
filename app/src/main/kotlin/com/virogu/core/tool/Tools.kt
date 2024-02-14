package com.virogu.core.tool

import com.virogu.core.command.AdbCommand
import com.virogu.core.command.BaseCommand
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.PingCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.manager.AdditionalManager
import com.virogu.core.tool.manager.FolderManager
import com.virogu.core.tool.manager.ProcessManager
import com.virogu.core.tool.manager.ScrcpyManager
import com.virogu.core.tool.scan.DeviceScan
import com.virogu.core.tool.ssh.SSHTool

interface Tools {
    val initTool: InitTool
    val configStores: ConfigStores
    val deviceScan: DeviceScan
    val logTool: LogTool

    val sshTool: SSHTool

    val baseCommand: BaseCommand
    val pingCommand: PingCommand
    val adbCommand: AdbCommand
    val hdcCommand: HdcCommand

    val scrcpyManager: ScrcpyManager
    val folderManager: FolderManager
    val processTool: ProcessManager
    val additionalManager: AdditionalManager

    fun start()
    fun stop()
}