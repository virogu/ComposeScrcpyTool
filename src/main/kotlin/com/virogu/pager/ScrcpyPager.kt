@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.virogu.bean.AdbDevice
import com.virogu.bean.Configs
import com.virogu.tools.Tools
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.scrcpy.ScrcpyTool


@Composable
fun ScrcpyView(tools: Tools) {
    val connectTool = tools.deviceConnectTool
    val scrcpyTool = tools.scrcpyTool
    val configTool = tools.configTool

    val scrcpyConfig = configTool.scrcpyConfigFlow.collectAsState()
    val current = connectTool.currentSelectedDevice.collectAsState()
    val isBusy = scrcpyTool.isBusy.collectAsState()

    val config = remember(scrcpyConfig, current) {
        mutableStateOf(current.value?.let {
            scrcpyConfig.value[it.serial]
        } ?: Configs.ScrcpyConfig())
    }

    Column {
        ScrcpyConfigView(configTool, config)
        ScrcpOptionView(scrcpyTool, isBusy, current, config)
    }
}

@Composable
private fun ScrcpyConfigView(
    configTool: ConfigTool,
    currentConfig: State<Configs.ScrcpyConfig>
) {

}

@Composable
private fun ScrcpOptionView(
    scrcpyTool: ScrcpyTool,
    isBusy: State<Boolean>,
    currentDevice: State<AdbDevice?>,
    currentConfig: State<Configs.ScrcpyConfig>
) {
    val activeDevices = scrcpyTool.activeDevicesFLow.collectAsState()
    val currentActive = remember(currentDevice.value, activeDevices.value) {
        mutableStateOf(currentDevice.value != null && activeDevices.value.contains(currentDevice.value?.serial))
    }
    Row {
        TextButton(
            onClick = {
                val device = currentDevice.value ?: return@TextButton
                scrcpyTool.connect(device.serial, currentConfig.value)
            },
            enabled = !isBusy.value && currentDevice.value != null && !currentActive.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("启动服务")
        }
        TextButton(
            onClick = {
                val device = currentDevice.value ?: return@TextButton
                scrcpyTool.disConnect(device.serial)
            },
            enabled = !isBusy.value && currentDevice.value != null && currentActive.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("断开服务")
        }
    }
}
