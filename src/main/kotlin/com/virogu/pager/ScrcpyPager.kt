@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.style.TextAlign
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

    val (commonConfig, _) = remember(scrcpyConfig.value.commonConfig) {
        mutableStateOf(scrcpyConfig.value.commonConfig)
    }

    val (specialConfig, _) = remember(scrcpyConfig.value.configs, currentDevice.value) {
        mutableStateOf(currentDevice.value?.let {
            scrcpyConfig.value.configs[it.serial]
        } ?: ScrcpyConfig.Config())
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "启动配置")
        ScrcpyConfigView(window, scrcpyConfig, commonConfig) {
            if (it != scrcpyConfig.value.commonConfig) {
                configTool.updateScrcpyConfig(it)
            }
        }
        ScrcpyOptionView(scrcpyTool, isBusy.value, commonConfig, currentDevice.value, specialConfig)
    }
}

@Composable
private fun ScrcpyConfigView(
    window: ComposeWindow,
    scrcpyConfig: State<ScrcpyConfig>,
    config: ScrcpyConfig.CommonConfig,
    updateCommonConfig: (ScrcpyConfig.CommonConfig) -> Unit,
) {
    val currentUpdateCommonConfig by rememberUpdatedState(updateCommonConfig)

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val modifier = Modifier.height(40.dp).align(Alignment.CenterVertically)
        SelectRecordPathView(modifier.weight(2f), window, config.recordPath) {
            currentUpdateCommonConfig(config.copy(recordPath = it))
        }
        RecordFormatView(modifier.weight(1f), config.recordFormat) {
            currentUpdateCommonConfig(config.copy(recordFormat = it))
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modifier = Modifier.weight(1f)
        CheckBoxView(
            "录制屏幕",
            checked = config.recordEnable,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(recordEnable = it))
        }
        CheckBoxView(
            "开启音频",
            checked = config.enableAudio,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(enableAudio = it))
        }
        CheckBoxView(
            "窗口置顶",
            checked = config.alwaysOnTop,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(alwaysOnTop = it))
        }
        CheckBoxView(
            "保持唤醒",
            checked = config.stayAwake,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(stayAwake = it))
        }

    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modifier = Modifier.weight(1f)
        CheckBoxView(
            "显示触摸",
            checked = config.showTouches,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(showTouches = it))
        }
        CheckBoxView(
            "设备息屏",
            checked = config.turnScreenOff,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(turnScreenOff = it))
        }
        CheckBoxView(
            "无边框",
            checked = config.noWindowBorder,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(config.copy(noWindowBorder = it))
        }
        Spacer(modifier)
    }
}

@Composable
private fun SelectRecordPathView(
    modifier: Modifier = Modifier,
    window: ComposeWindow,
    recordPath: String,
    onFileSelected: (String) -> Unit
) {
    val currentOnFileSelected by rememberUpdatedState(onFileSelected)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("录像路径", Modifier.width(80.dp).align(Alignment.CenterVertically))
        FileSelectView(
            window = window,
            text = recordPath,
            modifier = modifier,
            fileChooserType = JFileChooser.DIRECTORIES_ONLY,
            defaultPath = recordPath,
            multiSelectionEnabled = false
        ) {
            it.firstOrNull()?.path?.also { path ->
                currentOnFileSelected(path)
            }
        }
    }
}

@Composable
private fun RecordFormatView(
    modifier: Modifier = Modifier,
    recordFormat: ScrcpyConfig.RecordFormat,
    onFormatChanged: (ScrcpyConfig.RecordFormat) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val borderStroke = com.virogu.pager.view.animateBorderStrokeAsState()

    val dropMenuWidth = remember {
        mutableStateOf(0.dp)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("录制格式", Modifier.width(80.dp).align(Alignment.CenterVertically))
        Box(
            modifier = Modifier.clickable {
                expanded.value = true
            }.weight(1f).fillMaxHeight().border(
                borderStroke.value,
                TextFieldDefaults.OutlinedTextFieldShape
            ).onPlaced {
                dropMenuWidth.value = it.size.width.dp
            },
        ) {
            Text(
                text = recordFormat.value,
                modifier = Modifier.align(Alignment.Center).padding(0.dp),
                textAlign = TextAlign.Center
            )
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = {
                    expanded.value = false
                },
                modifier = Modifier.width(dropMenuWidth.value),
            ) {
                ScrcpyConfig.RecordFormat.values().forEach {
                    Text(text = it.value, modifier = Modifier.fillMaxWidth().clickable {
                        onFormatChanged(it)
                        expanded.value = false
                    }.padding(16.dp, 10.dp, 16.dp, 10.dp))
                }
            }
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
private fun ScrcpyOptionView(
    scrcpyTool: ScrcpyTool,
    isBusy: Boolean,
    commonConfig: ScrcpyConfig.CommonConfig,
    currentDevice: AdbDevice?,
    specialConfig: ScrcpyConfig.Config
) {
    val activeDevices = scrcpyTool.activeDevicesFLow.collectAsState()
    val currentActive = remember(currentDevice, activeDevices.value) {
        mutableStateOf(currentDevice != null && activeDevices.value.contains(currentDevice.serial))
    }
    Row {
        TextButton(
            onClick = {
                val device = currentDevice ?: return@TextButton
                scrcpyTool.connect(
                    device.serial,
                    device.showName,
                    commonConfig,
                    specialConfig
                )
            },
            enabled = !isBusy && currentDevice != null && !currentActive.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("启动服务")
        }
        TextButton(
            onClick = {
                val device = currentDevice ?: return@TextButton
                scrcpyTool.disConnect(device.serial)
            },
            enabled = !isBusy && currentDevice != null && currentActive.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("断开服务")
        }
    }
}
