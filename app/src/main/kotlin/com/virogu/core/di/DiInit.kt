package com.virogu.core.di

import com.virogu.core.command.AdbCommand
import com.virogu.core.command.BaseCommand
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.PingCommand
import com.virogu.core.config.*
import com.virogu.core.tool.ToolImpl
import com.virogu.core.tool.Tools
import com.virogu.core.tool.log.LogTool
import com.virogu.core.tool.manager.ScrcpyManager
import com.virogu.core.tool.scan.DeviceScan
import com.virogu.core.tool.ssh.SSHTool
import kotlinx.coroutines.flow.MutableSharedFlow
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
        bindSingleton<MutableSharedFlow<String>>("notification") {
            tools.notification
        }
        val configStores = tools.configStores
        bindSingleton<Tools> {
            tools
        }
        bindSingleton<BaseCommand> {
            tools.baseCommand
        }
        bindSingleton<PingCommand> {
            tools.pingCommand
        }
        bindSingleton<AdbCommand> {
            tools.adbCommand
        }
        bindSingleton<HdcCommand> {
            tools.hdcCommand
        }
        bindSingleton<ConfigStores> {
            configStores
        }
        bindSingleton<HistoryDevicesStore> {
            configStores.historyDevicesStore
        }
        bindSingleton<DeviceDescConfigStore> {
            configStores.deviceDescStore
        }
        bindSingleton<ScrcpyConfigStore> {
            configStores.scrcpyConfigStore
        }
        bindSingleton<SimpleConfigStore> {
            configStores.simpleConfigStore
        }
        bindSingleton<SSHTool> {
            tools.sshTool
        }
        bindSingleton<DeviceScan> {
            tools.deviceScan
        }
        bindSingleton<LogTool> {
            tools.logTool
        }
        bindSingleton<ScrcpyManager> {
            tools.scrcpyManager
        }
        onBind()
    }
}