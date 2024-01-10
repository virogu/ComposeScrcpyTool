package com.virogu.core.tool.impl

import com.virogu.core.PlateForm
import com.virogu.core.config.ConfigStores
import com.virogu.core.config.impl.ConfigStoreImpl
import com.virogu.core.currentPlateForm
import com.virogu.core.init.InitTool
import com.virogu.core.init.InitToolDefault
import com.virogu.core.init.InitToolLinux
import com.virogu.core.init.InitToolWindows
import com.virogu.core.tool.*

class ToolImpl : Tools {

    override val progressTool: ProgressTool = ProgressToolsImpl()

    override val configStores: ConfigStores = ConfigStoreImpl()

    override val logTool: LogTool = LogToolImpl()

    override val scrcpyTool: ScrcpyTool = ScrcpyToolImpl(progressTool)

    override val initTool: InitTool by lazy {
        when (currentPlateForm) {
            is PlateForm.Windows -> InitToolWindows()
            is PlateForm.Linux -> InitToolLinux(progressTool)
            else -> InitToolDefault()
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