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
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.init.InitToolDefault
import com.virogu.core.tool.init.InitToolLinux
import com.virogu.core.tool.init.InitToolWindows
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.log.LogToolImpl
import com.virogu.core.tool.manager.AdditionalManager
import com.virogu.core.tool.manager.FolderManager
import com.virogu.core.tool.manager.ProcessManager
import com.virogu.core.tool.manager.ScrcpyManager
import com.virogu.core.tool.manager.impl.AdditionalManagerImpl
import com.virogu.core.tool.manager.impl.FolderManagerImpl
import com.virogu.core.tool.manager.impl.ProcessManagerImpl
import com.virogu.core.tool.manager.impl.ScrcpyManagerImpl
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
        when (Common.platform) {
            is Platform.Windows -> InitToolWindows()
            is Platform.Linux -> InitToolLinux()
            else -> InitToolDefault()
        }
    }

    override val scrcpyManager: ScrcpyManager = ScrcpyManagerImpl()

    override val deviceConnect: DeviceConnect = DeviceConnectManager(initTool, configStores)

    override val folderManager: FolderManager = FolderManagerImpl(initTool, deviceConnect)

    override val processTool: ProcessManager = ProcessManagerImpl(initTool, deviceConnect)

    override val additionalManager: AdditionalManager = AdditionalManagerImpl(deviceConnect)

    override fun start() {
        logTool.start()
        initTool.init()
        deviceConnect.start()
    }

    override fun stop() {
        scrcpyManager.disConnect()
        deviceConnect.stop()
        baseCommand.destroy()
        pingCommand.destroy()
        hdcCommand.destroy()
        adbCommand.destroy()
        logTool.stop()
    }

}