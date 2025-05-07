/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

@file:Suppress("FunctionName")

package com.virogu.ui.pager

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.device.Device
import com.virogu.core.tool.Tools
import com.virogu.core.viewmodel.ScrcpyViewModel
import com.virogu.ui.view.FileSelectView
import theme.Icon
import theme.materialColors
import theme.textFieldHeight
import views.OutlinedText
import javax.swing.JFileChooser

@Composable
fun ScrcpyView(
    tools: Tools,
    viewModel: ScrcpyViewModel = viewModel { ScrcpyViewModel() }
) {
    val connectTool = tools.deviceConnect
    val configTool = tools.configStores.scrcpyConfigStore

    val scrcpyConfig = configTool.scrcpyConfigFlow.collectAsState()
    val currentDevice = connectTool.currentSelectedDevice.collectAsState()
    val isBusy = viewModel.isBusy.collectAsState()

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
        ScrcpyOptionView(viewModel, isBusy.value, commonConfig, currentDevice.value, specialConfig)
    }
}

@Composable
private fun ScrcpyConfigView(
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
        val modifier = Modifier.textFieldHeight().align(Alignment.CenterVertically)
        SelectRecordPathView(modifier.weight(2f), commonConfig.recordPath) {
            currentUpdateCommonConfig(commonConfig.copy(recordPath = it))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val modifier = Modifier.height(40.dp).weight(1f).align(Alignment.CenterVertically)
        val labelModifier = Modifier.width(80.dp).align(Alignment.CenterVertically)
        DropMenuConfigView(
            modifier,
            label = {
                Text("录像格式", labelModifier)
            },
            currentValue = commonConfig.recordFormat,
            menuList = ScrcpyConfig.RecordFormat.entries,
            valueFormat = { it.value },
        ) {
            currentUpdateCommonConfig(commonConfig.copy(recordFormat = it))
        }
        DropMenuConfigView(
            modifier,
            label = {
                Text("视频编码", labelModifier)
            },
            currentValue = specialConfig.videoCodec,
            menuList = ScrcpyConfig.VideoCodec.entries,
            valueFormat = { it.value },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(videoCodec = it))
        }
        DropMenuConfigView(
            modifier,
            label = {
                Text("比特率", labelModifier)
            },
            currentValue = specialConfig.bitRate,
            menuList = ScrcpyConfig.VideoBiteRate.entries,
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
        val labelModifier = Modifier.width(80.dp).align(Alignment.CenterVertically)
        DropMenuConfigView(
            modifier,
            label = {
                Text("最大尺寸", labelModifier)
            },
            currentValue = specialConfig.maxSize,
            menuList = ScrcpyConfig.MaxSize.entries,
            valueFormat = { it.value },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(maxSize = it))
        }

        DropMenuConfigView(
            modifier,
            label = {
                Text("视频方向", labelModifier)
            },
            currentValue = specialConfig.videoRotation,
            menuList = ScrcpyConfig.VideoRotation.entries,
            valueFormat = { it.desc },
            enabled = specialConfigEnable,
        ) {
            currentUpdateSpecialConfig(specialConfig.copy(videoRotation = it))
        }
        DropMenuConfigView(
            modifier,
            label = {
                Text("窗口方向", labelModifier)
            },
            currentValue = specialConfig.windowRotation,
            menuList = ScrcpyConfig.WindowRotation.entries,
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

@OptIn(ExperimentalMaterialApi::class)
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

    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        label()
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (!enabled) {
                    return@ExposedDropdownMenuBox
                }
                expanded = !expanded
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            OutlinedText(
                modifier = Modifier.fillMaxSize().align(Alignment.CenterVertically),
                textAlign = TextAlign.Center,
                enabled = enabled,
                value = currentValueFormat(currentValue),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                menuList.forEach {
                    Text(
                        text = currentValueFormat(it),
                        style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().clickable {
                            currentOnMenuSelected(it)
                            expanded = false
                        }.padding(16.dp, 8.dp)
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

@Composable
private fun ColumnScope.ScrcpyOptionView(
    viewModel: ScrcpyViewModel,
    isBusy: Boolean,
    commonConfig: ScrcpyConfig.CommonConfig,
    currentDevice: Device?,
    specialConfig: ScrcpyConfig.Config
) {
    val activeDevices = viewModel.activeDevicesFLow.collectAsState()
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
                viewModel.disConnect(device)
            } else {
                viewModel.connect(device, commonConfig, specialConfig)
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
                scaleIn(tween(200, 200)) togetherWith scaleOut(tween(200))
                //slideInHorizontally(tween(200)) { -it } with slideOutHorizontally(tween(200)) { it }
            }
        ) { active ->
            Icon(
                painter = if (active) {
                    Icon.Outlined.Close
                } else {
                    Icon.Outlined.ArrowForward
                },
                contentDescription = if (active) "断开服务" else "启动服务",
                tint = contentColorFor(buttonColor)
            )
        }
    }
}
