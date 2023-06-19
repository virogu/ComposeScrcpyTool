package com.virogu.tools

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.common.AuxiliaryTool
import com.virogu.tools.config.ConfigStores
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.explorer.FileExplorer
import com.virogu.tools.init.InitTool
import com.virogu.tools.log.LogTool
import com.virogu.tools.process.DeviceProcessTool
import com.virogu.tools.scrcpy.ScrcpyTool

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