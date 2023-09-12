package com.virogu.tools

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.adb.ProgressToolsImpl
import com.virogu.tools.common.AuxiliaryTool
import com.virogu.tools.common.AuxiliaryToolImpl
import com.virogu.tools.config.ConfigStores
import com.virogu.tools.config.impl.ConfigStoreImpl
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.connect.DeviceConnectToolImpl
import com.virogu.tools.explorer.FileExplorer
import com.virogu.tools.explorer.FileExplorerImpl
import com.virogu.tools.init.DefaultInitTool
import com.virogu.tools.init.InitTool
import com.virogu.tools.init.LinuxInitTool
import com.virogu.tools.init.WindowsInitTool
import com.virogu.tools.log.LogTool
import com.virogu.tools.log.LogToolImpl
import com.virogu.tools.process.DeviceProcessTool
import com.virogu.tools.process.DeviceProcessToolImpl
import com.virogu.tools.scrcpy.ScrcpyTool
import com.virogu.tools.scrcpy.ScrcpyToolImpl

class ToolImpl : Tools {

    override val progressTool: ProgressTool = ProgressToolsImpl()

    override val configStores: ConfigStores = ConfigStoreImpl()

    override val logTool: LogTool = LogToolImpl()

    override val scrcpyTool: ScrcpyTool = ScrcpyToolImpl(progressTool)


    override val initTool: InitTool by lazy {
        when (currentPlateForm) {
            is PlateForm.Windows -> WindowsInitTool()
            is PlateForm.Linux -> LinuxInitTool(progressTool)
            else -> DefaultInitTool()
        }
    }

    override val deviceConnectTool: DeviceConnectTool = DeviceConnectToolImpl(
        initTool = initTool,
        configStores = configStores,
        progressTool = progressTool,
    )

    override val fileExplorer: FileExplorer = FileExplorerImpl(initTool, deviceConnectTool, progressTool)

    override val processTool: DeviceProcessTool = DeviceProcessToolImpl(initTool, deviceConnectTool, progressTool)

    override val auxiliaryTool: AuxiliaryTool = AuxiliaryToolImpl(deviceConnectTool, progressTool)

    init {
        initTool.init()
    }

    override fun start() {
        logTool.start()
    }

    override fun stop() {
        scrcpyTool.disConnect()
        progressTool.destroy()
        logTool.stop()
    }

}