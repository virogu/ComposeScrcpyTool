@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.virogu.bean.AdbDevice
import com.virogu.bean.ScrcpyConfig
import com.virogu.pager.view.FileSelectView
import com.virogu.tools.Tools
import com.virogu.tools.scrcpy.ScrcpyTool
import theme.materialColors
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
        ScrcpyConfigView(
            window = window,
            specialConfig = specialConfig,
            commonConfig = commonConfig,
            specialConfigEnable = currentDevice.value != null,
            updateCommonConfig = label@{
                if (it == commonConfig) {
                    return@label
                }
                configTool.updateScrcpyConfig(it)
            },
            updateSpecialConfig = label@{
                val device = currentDevice.value ?: return@label
                if (it == specialConfig) {
                    return@label
                }
                configTool.updateScrcpyConfig(device.serial, it)
            }
        )
        ScrcpyOptionView(scrcpyTool, isBusy.value, commonConfig, currentDevice.value, specialConfig)
    }
}

@Composable
private fun ScrcpyConfigView(
    window: ComposeWindow,
    specialConfig: ScrcpyConfig.Config,
    commonConfig: ScrcpyConfig.CommonConfig,
    specialConfigEnable: Boolean,
    updateCommonConfig: (ScrcpyConfig.CommonConfig) -> Unit,
    updateSpecialConfig: (ScrcpyConfig.Config) -> Unit,
) {
    val currentUpdateCommonConfig by rememberUpdatedState(updateCommonConfig)
    val currentUpdateSpecialConfig by rememberUpdatedState(updateSpecialConfig)
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val modifier = Modifier.height(40.dp).align(Alignment.CenterVertically)
        SelectRecordPathView(modifier.weight(2f), window, commonConfig.recordPath) {
            currentUpdateCommonConfig(commonConfig.copy(recordPath = it))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val modifier = Modifier.height(40.dp).weight(1f).align(Alignment.CenterVertically)
        DropMenuConfigView(
            modifier,
            label = {
                Text("录像格式", Modifier.width(80.dp).align(Alignment.CenterVertically))
            },
            currentValue = commonConfig.recordFormat,
            menuList = ScrcpyConfig.RecordFormat.values().toList(),
            valueFormat = { it.value },
        ) {
            currentUpdateCommonConfig(commonConfig.copy(recordFormat = it))
        }
        DropMenuConfigView(
            modifier,
            label = {
                Text("视频编码", Modifier.width(80.dp).align(Alignment.CenterVertically))
            },
            currentValue = specialConfig.videoCodec,
            menuList = ScrcpyConfig.VideoCodec.values().toList(),
            valueFormat = { it.value },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(videoCodec = it))
        }
        DropMenuConfigView(
            modifier,
            label = {
                Text("比特率", Modifier.width(80.dp).align(Alignment.CenterVertically))
            },
            currentValue = specialConfig.bitRate,
            menuList = ScrcpyConfig.VideoBiteRate.values().toList(),
            valueFormat = { it.value },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(bitRate = it))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val modifier = Modifier.height(40.dp).weight(1f).align(Alignment.CenterVertically)
        DropMenuConfigView(
            modifier,
            label = {
                Text("最大尺寸", Modifier.width(80.dp).align(Alignment.CenterVertically))
            },
            currentValue = specialConfig.maxSize,
            menuList = ScrcpyConfig.MaxSize.values().toList(),
            valueFormat = { it.value },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(maxSize = it))
        }

        DropMenuConfigView(
            modifier,
            label = {
                Text("视频方向", Modifier.width(80.dp).align(Alignment.CenterVertically))
            },
            currentValue = specialConfig.videoRotation,
            menuList = ScrcpyConfig.VideoRotation.values().toList(),
            valueFormat = { it.desc },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(videoRotation = it))
        }
        DropMenuConfigView(
            modifier,
            label = {
                Text("窗口方向", Modifier.width(80.dp).align(Alignment.CenterVertically))
            },
            currentValue = specialConfig.windowRotation,
            menuList = ScrcpyConfig.WindowRotation.values().toList(),
            valueFormat = { it.desc },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(windowRotation = it))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modifier = Modifier.weight(1f)
        CheckBoxView(
            "录制屏幕",
            checked = commonConfig.recordEnable,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(recordEnable = it))
        }
        CheckBoxView(
            "开启音频",
            checked = commonConfig.enableAudio,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(enableAudio = it))
        }
        CheckBoxView(
            "窗口置顶",
            checked = commonConfig.alwaysOnTop,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(alwaysOnTop = it))
        }
        CheckBoxView(
            "保持唤醒",
            checked = commonConfig.stayAwake,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(stayAwake = it))
        }

    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modifier = Modifier.weight(1f)
        CheckBoxView(
            "显示触摸",
            checked = commonConfig.showTouches,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(showTouches = it))
        }
        CheckBoxView(
            "设备息屏",
            checked = commonConfig.turnScreenOff,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(turnScreenOff = it))
        }
        CheckBoxView(
            "无边框",
            checked = commonConfig.noWindowBorder,
            modifier = modifier
        ) {
            currentUpdateCommonConfig(commonConfig.copy(noWindowBorder = it))
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
private fun <T> DropMenuConfigView(
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
    currentValue: T,
    menuList: Collection<T>,
    valueFormat: (T) -> String,
    enabled: Boolean = true,
    onMenuSelected: (T) -> Unit
) {
    val currentOnMenuSelected by rememberUpdatedState(onMenuSelected)
    val currentValueFormat by rememberUpdatedState(valueFormat)

    val expanded = remember { mutableStateOf(false) }
    val borderStroke = com.virogu.pager.view.animateBorderStrokeAsState()

    val dropMenuWidth = remember {
        mutableStateOf(0.dp)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        label()
        Box(
            modifier = Modifier.clickable(enabled) {
                expanded.value = true
            }.weight(1f).background(
                if (enabled) {
                    Color.Transparent
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                },
                TextFieldDefaults.OutlinedTextFieldShape
            ).fillMaxHeight().border(
                borderStroke.value,
                TextFieldDefaults.OutlinedTextFieldShape
            ).onPlaced {
                dropMenuWidth.value = it.size.width.dp
            },
        ) {
            Text(
                text = currentValueFormat(currentValue),
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
                menuList.forEach {
                    Text(
                        text = currentValueFormat(it),
                        modifier = Modifier.fillMaxWidth().clickable {
                            currentOnMenuSelected(it)
                            expanded.value = false
                        }.padding(16.dp, 10.dp, 16.dp, 10.dp)
                    )
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ColumnScope.ScrcpyOptionView(
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
    val colors = materialColors

    val buttonEnabled by remember(isBusy, currentDevice) {
        mutableStateOf(!isBusy && currentDevice != null && currentDevice.isOnline)
    }

    val buttonColor by animateColorAsState(
        targetValue = remember(currentDevice, currentActive.value) {
            val color = if (currentDevice == null) {
                colors.onSurface.copy(alpha = 0.12f)
            } else {
                if (currentActive.value) {
                    Color.Red
                } else {
                    colors.primary
                }
            }
            mutableStateOf(color)
        }.value
    )
    Button(
        onClick = label@{
            val device = currentDevice ?: return@label
            if (currentActive.value) {
                scrcpyTool.disConnect(device.serial)
            } else {
                scrcpyTool.connect(device.serial, device.showName, commonConfig, specialConfig)
            }
        },
        modifier = Modifier.size(50.dp).align(Alignment.CenterHorizontally),
        enabled = buttonEnabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
        contentPadding = PaddingValues(8.dp)
    ) {
        AnimatedContent(
            targetState = currentActive.value,
            transitionSpec = {
                scaleIn(tween(200, 200)) with scaleOut(tween(200))
                //slideInHorizontally(tween(200)) { -it } with slideOutHorizontally(tween(200)) { it }
            }
        ) { active ->
            Icon(
                imageVector = if (active) {
                    Icons.Filled.Close
                } else {
                    Icons.Filled.ArrowForward
                },
                contentDescription = if (active) "断开服务" else "启动服务",
                tint = contentColorFor(buttonColor)
            )
        }
    }
}
