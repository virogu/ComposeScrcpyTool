package com.virogu.tools

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.log.LogTool
import com.virogu.tools.scrcpy.ScrcpyTool
import com.virogu.tools.sshd.SSHTool

interface Tools {
    val progressTool: ProgressTool
    val sshTool: SSHTool
    val configTool: ConfigTool
    val deviceConnectTool: DeviceConnectTool
    val logTool: LogTool
    val scrcpyTool: ScrcpyTool
}