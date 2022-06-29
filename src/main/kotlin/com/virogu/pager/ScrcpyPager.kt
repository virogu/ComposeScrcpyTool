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

    val (commonConfig, updateCommonConfig) = remember {
        mutableStateOf(scrcpyConfig.value.commonConfig)
    }
    LaunchedEffect(commonConfig) {
        if (commonConfig != configTool.scrcpyConfigFlow.value.commonConfig) {
            configTool.updateScrcpyConfig(commonConfig)
        }
    }
    val (specialConfig, updateSpecialConfig) = remember(scrcpyConfig.value, currentDevice.value) {
        mutableStateOf(currentDevice.value?.let {
            scrcpyConfig.value.configs[it.serial]
        } ?: ScrcpyConfig.Config())
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "启动配置")
        ScrcpyConfigView(window, commonConfig, updateCommonConfig, specialConfig)
        ScrcpOptionView(scrcpyTool, isBusy, commonConfig, currentDevice, specialConfig)
    }
}

@Composable
private fun ScrcpyConfigView(
    window: ComposeWindow,
    commonConfig: ScrcpyConfig.CommonConfig,
    updateCommonConfig: (ScrcpyConfig.CommonConfig) -> Unit,
    specialConfig: ScrcpyConfig.Config
) {
    FileSelectView(
        window = window,
        label = "录像路径",
        text = commonConfig.recordPath,
        fileChooserType = JFileChooser.DIRECTORIES_ONLY,
        defaultPath = commonConfig.recordPath,
        multiSelectionEnabled = false
    ) {
        it.firstOrNull()?.path?.also { path ->
            updateCommonConfig(commonConfig.copy(recordPath = path))
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckBoxView(
            "录制屏幕",
            checked = commonConfig.recordEnable,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(recordEnable = it))
        }
        CheckBoxView(
            "开启音频",
            checked = commonConfig.enableAudio,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(enableAudio = it))
        }
        CheckBoxView(
            "窗口置顶",
            checked = commonConfig.alwaysOnTop,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(alwaysOnTop = it))
        }
        Spacer(Modifier.weight(1f))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckBoxView(
            "设备息屏",
            checked = commonConfig.turnScreenOff,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(turnScreenOff = it))
        }
        CheckBoxView(
            "保持唤醒",
            checked = commonConfig.stayAwake,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(stayAwake = it))
        }
        CheckBoxView(
            "显示触摸",
            checked = commonConfig.showTouches,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(showTouches = it))
        }
        CheckBoxView(
            "无边框",
            checked = commonConfig.noWindowBorder,
            modifier = Modifier.weight(1f)
        ) {
            updateCommonConfig(commonConfig.copy(noWindowBorder = it))
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
    commonConfig: ScrcpyConfig.CommonConfig,
    currentDevice: State<AdbDevice?>,
    currentConfig: ScrcpyConfig.Config
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
                    commonConfig,
                    currentConfig
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
