package com.virogu.tools

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.init.InitTool
import com.virogu.tools.log.LogTool
import com.virogu.tools.scrcpy.ScrcpyTool

interface Tools {
    val initTool: InitTool
    val progressTool: ProgressTool
    val configTool: ConfigTool
    val deviceConnectTool: DeviceConnectTool
    val logTool: LogTool
    val scrcpyTool: ScrcpyTool
}