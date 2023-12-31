package com.virogu.di

import com.virogu.tools.*
import com.virogu.tools.config.*
import com.virogu.tools.log.LogTool
import com.virogu.tools.sshd.SSHTool
import com.virogu.tools.sshd.SSHToolImpl
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.conf.global

@Volatile
private var init = false

@Synchronized
fun initDi(
    onBind: DI.MainBuilder.() -> Unit = {}
) {
    if (init) {
        return
    }
    init = true
    DI.global.addConfig {
        val tools = ToolImpl()
        bindSingleton<SSHTool> {
            SSHToolImpl()
        }
        bindSingleton<Tools> {
            tools
        }
        bindSingleton<ProgressTool> {
            tools.progressTool
        }
        bindSingleton<ConfigStores> {
            tools.configStores
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
        bindSingleton<HistoryDevicesStore> {
            tools.configStores.historyDevicesStore
        }
        bindSingleton<DeviceDescConfigStore> {
            tools.configStores.deviceDescStore
        }
        bindSingleton<ScrcpyConfigStore> {
            tools.configStores.scrcpyConfigStore
        }
        bindSingleton<SimpleConfigStore> {
            tools.configStores.simpleConfigStore
        }
        onBind()
    }
}