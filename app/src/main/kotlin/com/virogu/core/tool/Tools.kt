package com.virogu.core.tool

import com.virogu.core.config.ConfigStores
import com.virogu.core.init.InitTool

interface Tools {
    val initTool: InitTool
    val progressTool: ProgressTool
    val configStores: ConfigStores
    val deviceConnectTool: DeviceConnectTool
    val logTool: LogTool
    val scrcpyTool: ScrcpyTool
    val fileExplorer: FileExplorer
    val processTool: DeviceProcessTool
    val auxiliaryTool: AuxiliaryTool

    fun start()
    fun stop()
}