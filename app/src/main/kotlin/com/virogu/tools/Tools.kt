package com.virogu.tools

import com.virogu.tools.config.ConfigStores
import com.virogu.tools.init.InitTool
import com.virogu.tools.log.LogTool

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