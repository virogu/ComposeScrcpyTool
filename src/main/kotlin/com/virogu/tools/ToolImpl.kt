package com.virogu.tools

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.adb.ProgressToolsImpl
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.config.ConfigToolImpl
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.connect.DeviceConnectToolImpl
import com.virogu.tools.init.DefaultInitTool
import com.virogu.tools.init.InitTool
import com.virogu.tools.init.LinuxInitTool
import com.virogu.tools.init.WindowsInitTool
import com.virogu.tools.log.LogTool
import com.virogu.tools.log.LogToolImpl
import com.virogu.tools.scrcpy.ScrcpyTool
import com.virogu.tools.scrcpy.ScrcpyToolImpl

class ToolImpl : Tools {

    override val progressTool: ProgressTool = ProgressToolsImpl()

    override val configTool: ConfigTool = ConfigToolImpl()

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
        configTool = configTool,
        progressTool = progressTool,
    )

    init {
        initTool.init()
    }

}