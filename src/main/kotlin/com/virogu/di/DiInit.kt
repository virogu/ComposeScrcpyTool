package com.virogu.di

import com.virogu.tools.ToolImpl
import com.virogu.tools.Tools
import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.log.LogTool
import com.virogu.tools.scrcpy.ScrcpyTool
import com.virogu.tools.sshd.SSHTool
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.conf.global

fun initDi(
    onBind: DI.MainBuilder.() -> Unit = {}
) {
    DI.global.addConfig {
        val tools = ToolImpl()
        bindSingleton<Tools> {
            tools
        }
        bindSingleton<ProgressTool> {
            tools.progressTool
        }
        bindSingleton<SSHTool> {
            tools.sshTool
        }
        bindSingleton<ConfigTool> {
            tools.configTool
        }
        bindSingleton<DeviceConnectTool> {
            tools.deviceConnectTool
        }
        bindSingleton<LogTool> {
            tools.logTool
        }
        bindSingleton<ScrcpyTool> {
            tools.scrcpyTool
        }
        onBind()
    }
}