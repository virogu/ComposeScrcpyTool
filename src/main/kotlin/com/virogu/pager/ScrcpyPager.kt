@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import com.virogu.bean.AdbDevice
import com.virogu.bean.ScrcpyConfig
import com.virogu.pager.view.FileSelectView
import com.virogu.tools.Tools
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.scrcpy.ScrcpyTool
import javax.swing.JFileChooser

@Composable
fun ScrcpyView(window: ComposeWindow, tools: Tools) {
    val connectTool = tools.deviceConnectTool
    val scrcpyTool = tools.scrcpyTool
    val configTool = tools.configTool

    val scrcpyConfig = configTool.scrcpyConfigFlow.collectAsState()
    val currentDevice = connectTool.currentSelectedDevice.collectAsState()
    val isBusy = scrcpyTool.isBusy.collectAsState()

    val commonConfig = remember(scrcpyConfig.value.commonConfig) {
        mutableStateOf(scrcpyConfig.value.commonConfig)
    }
    val currentConfig = remember(scrcpyConfig.value.configs, currentDevice.value) {
        mutableStateOf(currentDevice.value?.let {
            scrcpyConfig.value.configs[it.serial]
        } ?: ScrcpyConfig.Config())
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "启动配置")
        ScrcpyConfigView(window, configTool, commonConfig, currentConfig)
        ScrcpOptionView(scrcpyTool, isBusy, commonConfig, currentDevice, currentConfig)
    }
}

@Composable
private fun ScrcpyConfigView(
    window: ComposeWindow,
    configTool: ConfigTool,
    commonConfig: MutableState<ScrcpyConfig.CommonConfig>,
    currentConfig: MutableState<ScrcpyConfig.Config>
) {
    LaunchedEffect(commonConfig.value) {
        configTool.updateScrcpyConfig(commonConfig.value)
    }
    FileSelectView(
        window = window,
        label = "录像路径",
        text = commonConfig.value.recordPath,
        fileChooserType = JFileChooser.DIRECTORIES_ONLY,
        defaultPath = commonConfig.value.recordPath,
        multiSelectionEnabled = false
    ) {
        it.firstOrNull()?.path?.also { path ->
            commonConfig.value = commonConfig.value.copy(recordPath = path)
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckBoxView(
            "录制屏幕",
            checked = commonConfig.value.recordEnable,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(recordEnable = it)
        }
        CheckBoxView(
            "开启音频",
            checked = commonConfig.value.enableAudio,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(enableAudio = it)
        }
        CheckBoxView(
            "窗口置顶",
            checked = commonConfig.value.alwaysOnTop,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(alwaysOnTop = it)
        }
        Spacer(Modifier.weight(1f))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckBoxView(
            "设备息屏",
            checked = commonConfig.value.turnScreenOff,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(turnScreenOff = it)
        }
        CheckBoxView(
            "保持唤醒",
            checked = commonConfig.value.stayAwake,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(stayAwake = it)
        }
        CheckBoxView(
            "显示触摸",
            checked = commonConfig.value.showTouches,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(showTouches = it)
        }
        CheckBoxView(
            "无边框",
            checked = commonConfig.value.noWindowBorder,
            modifier = Modifier.weight(1f)
        ) {
            commonConfig.value = commonConfig.value.copy(noWindowBorder = it)
        }
    }

}

@Composable
private fun CheckBoxView(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier.width(80.dp),
    onCheckedChange: ((Boolean) -> Unit) = {},
) {
    Row(
        modifier = modifier.clickable {
            onCheckedChange(!checked)
        }
    ) {
        Checkbox(
            checked = checked,
            modifier = Modifier.align(Alignment.CenterVertically),
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.primary
            )
        )
        Text(title, modifier = textModifier.align(Alignment.CenterVertically))
    }
}

@Composable
private fun ScrcpOptionView(
    scrcpyTool: ScrcpyTool,
    isBusy: State<Boolean>,
    commonConfig: State<ScrcpyConfig.CommonConfig>,
    currentDevice: State<AdbDevice?>,
    currentConfig: State<ScrcpyConfig.Config>
) {
    val activeDevices = scrcpyTool.activeDevicesFLow.collectAsState()
    val currentActive = remember(currentDevice.value, activeDevices.value) {
        mutableStateOf(currentDevice.value != null && activeDevices.value.contains(currentDevice.value?.serial))
    }
    Row {
        TextButton(
            onClick = {
                val device = currentDevice.value ?: return@TextButton
                scrcpyTool.connect(
                    device.serial,
                    device.showName,
                    commonConfig.value,
                    currentConfig.value
                )
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
