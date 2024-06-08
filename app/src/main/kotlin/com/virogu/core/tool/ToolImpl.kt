package com.virogu.core.tool

import com.virogu.core.PlateForm
import com.virogu.core.command.AdbCommand
import com.virogu.core.command.BaseCommand
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.PingCommand
import com.virogu.core.config.ConfigStores
import com.virogu.core.config.impl.ConfigStoreImpl
import com.virogu.core.currentPlateForm
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.init.InitToolDefault
import com.virogu.core.tool.init.InitToolLinux
import com.virogu.core.tool.init.InitToolWindows
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.log.LogToolImpl
import com.virogu.core.tool.manager.*
import com.virogu.core.tool.scan.DeviceScan
import com.virogu.core.tool.scan.DeviceScanManager
import com.virogu.core.tool.ssh.SSHTool
import com.virogu.core.tool.ssh.SSHToolImpl
import kotlinx.coroutines.flow.MutableSharedFlow

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
        when (currentPlateForm) {
            is PlateForm.Windows -> InitToolWindows()
            is PlateForm.Linux -> InitToolLinux()
            else -> InitToolDefault()
        }
    }

    override val scrcpyManager: ScrcpyManager = ScrcpyManagerImpl()

    override val deviceScan: DeviceScan = DeviceScanManager(initTool, configStores)

    override val folderManager: FolderManager = FolderManagerImpl(initTool, deviceScan)

    override val processTool: ProcessManager = ProcessManagerImpl(initTool, deviceScan)

    override val additionalManager: AdditionalManager = AdditionalManagerImpl(deviceScan)

    override fun start() {
        logTool.start()
        initTool.init()
        deviceScan.start()
    }

    override fun stop() {
        scrcpyManager.disConnect()
        deviceScan.stop()
        baseCommand.destroy()
        pingCommand.destroy()
        hdcCommand.destroy()
        adbCommand.destroy()
        logTool.stop()
    }

}