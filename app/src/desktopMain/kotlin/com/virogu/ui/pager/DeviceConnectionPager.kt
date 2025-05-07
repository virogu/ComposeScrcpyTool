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
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virogu.core.bean.HistoryDevice
import com.virogu.core.tool.Tools
import com.virogu.core.tool.log.LogTool
import com.virogu.ui.view.LogListView
import com.virogu.ui.view.SelectDeviceView
import logger
import theme.*
import views.OutlinedTextField
import views.modifier.onEnterKey

/**
 * @author Virogu
 * @since 2022-08-31 15:40
 **/
@Composable
fun DeviceConnectView(
    tools: Tools,
    deviceConnectListState: LazyListState,
) {
    //val coroutineScope = rememberCoroutineScope()
    val scrollAdapter = rememberScrollbarAdapter(deviceConnectListState)
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "设备连接")
        Row(modifier = Modifier.weight(5f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                state = deviceConnectListState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ConnectDeviceView(tools)
                }
                item {
                    DeviceListView(tools)
                }
                item {
                    DeviceView(tools)
                }
                item {
                    ScrcpyView(tools)
                }
            }
            VerticalScrollbar(
                modifier = Modifier,
                adapter = scrollAdapter,
                reverseLayout = false,
            )
        }
        LogView(tools.logTool)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConnectDeviceView(
    tools: Tools
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val historyDevicesStore = tools.configStores.historyDevicesStore
        val connectTool = tools.deviceConnect
        val isBusy = connectTool.isBusy.collectAsState()
        val history = historyDevicesStore.historyDeviceFlow.collectAsState()

        val lastConnectedDevice = remember(history.value) {
            history.value.maxByOrNull { it.timeMs }
        }
        val ip = remember {
            mutableStateOf(lastConnectedDevice?.ip.orEmpty())
        }
        val port = remember {
            mutableStateOf(lastConnectedDevice?.port ?: 5555)
        }

        //for drop menu
        var expanded by remember { mutableStateOf(false) }
        val connectAction = label@{
            if (isBusy.value) {
                return@label
            }
            val ipString = ip.value
            if (ipString.isEmpty()) {
                return@label
            }
            val valid = ipString.split(".").takeIf {
                it.size == 4
            }?.let {
                it.forEach { s ->
                    s.toIntOrNull() ?: run {
                        return@let false
                    }
                }
                true
            } ?: false
            if (!valid) {
                logger.info { "IP地址[$ipString]无效" }
                return@label
            }
            historyDevicesStore.updateLastConnect(
                HistoryDevice(
                    System.currentTimeMillis(),
                    ip.value,
                    port.value
                )
            )
            connectTool.connect(ip.value, port.value)
        }

        val ipModifier = Modifier.textFieldWithLabelHeight().onEnterKey {
            connectAction()
        }
        Text("无线连接", modifier = Modifier.width(80.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                //expanded = !expanded
            },
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ip.value,
                    onValueChange = {
                        ip.value = it.filter { c: Char -> c.isDigit() || c == '.' }.take(15)
                    },
                    placeholder = {
                        Text("192.168.5.1")
                    },
                    singleLine = true,
                    modifier = ipModifier.weight(3f),
                    label = {
                        Text(text = "IP")
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = contentColorFor(MaterialTheme.colors.background)),
                )
                OutlinedTextField(
                    value = port.value.toString(),
                    onValueChange = {
                        port.value = it.take(5).toIntOrNull() ?: 5555
                    },
                    placeholder = {
                        Text("5555")
                    },
                    singleLine = true,
                    modifier = ipModifier.weight(2f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    label = {
                        Text(text = "端口")
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = contentColorFor(MaterialTheme.colors.background)),
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded, onIconClick = { expanded = !expanded })
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                history.value.forEach { device ->
                    DropdownMenuItem(
                        onClick = {
                            ip.value = device.ip
                            port.value = device.port
                            expanded = false
                        },
                        contentPadding = dropdownMenuItemPadding(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = device.showName,
                                modifier = Modifier.weight(1f)
                            )
                            IconToggleButton(
                                checked = device.tagged,
                                onCheckedChange = {
                                    historyDevicesStore.updateLastConnectTagged(device, it)
                                }
                            ) {
                                Icon(
                                    painter = if (device.tagged) {
                                        Icon.Filled.Star
                                    } else {
                                        Icon.Outlined.Star
                                    },
                                    contentDescription = if (device.tagged) "取消置顶" else "置顶",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton({
                                historyDevicesStore.removeLastConnect(device)
                            }) {
                                Icon(
                                    Icon.Outlined.Close,
                                    "",
                                    tint = contentColorFor(MaterialTheme.colors.background),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        TextButton(
            onClick = connectAction,
            enabled = !isBusy.value,
        ) {
            Text("无线连接")
        }
    }

}

@Composable
fun DeviceListView(
    tools: Tools,
) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        val connectTool = tools.deviceConnect
        val isBusy = connectTool.isBusy.collectAsState()
        val current = connectTool.currentSelectedDevice.collectAsState()
        Text("设备列表", modifier = Modifier.width(80.dp).align(Alignment.CenterVertically))
        SelectDeviceView(
            modifier = Modifier.textFieldHeight().weight(1f).align(Alignment.CenterVertically),
            currentDevice = current.value,
            connectTool = connectTool
        )
        TextButton(
            onClick = connectTool::refresh,
            enabled = !isBusy.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("刷新设备")
        }
    }
}

@Composable
fun DeviceView(tools: Tools) {
    val connectTool = tools.deviceConnect
    val isBusy = connectTool.isBusy.collectAsState()
    val current = connectTool.currentSelectedDevice.collectAsState()

    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        val desc = remember(current.value) {
            mutableStateOf(current.value?.desc.orEmpty())
        }
        Text("设备名称", modifier = Modifier.width(80.dp).align(Alignment.CenterVertically))
        val updateDescAction = label@{
            if (isBusy.value) {
                return@label
            }
            current.value ?: return@label
            connectTool.updateCurrentDesc(desc.value)
        }
        OutlinedTextField(
            value = desc.value,
            modifier = Modifier.textFieldHeight().onEnterKey {
                updateDescAction()
            }.weight(1f),
            singleLine = true,
            onValueChange = {
                desc.value = it.take(20)
            },
            placeholder = {
                Text(
                    current.value?.model.orEmpty().ifEmpty { "Phone" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
        TextButton(
            onClick = updateDescAction,
            enabled = !isBusy.value && current.value != null,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("更新备注")
        }
        TextButton(
            onClick = {
                current.value?.also {
                    connectTool.disconnect(it)
                }
            },
            enabled = !isBusy.value && current.value != null,
            colors = ButtonDefaults.textButtonColors(contentColor = Red_500),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("断开连接")
        }
        TextButton(
            onClick = {
                connectTool.disconnectAll()
            },
            enabled = !isBusy.value,
            colors = ButtonDefaults.textButtonColors(contentColor = Red_500),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("全部断开")
        }
    }
}

@Composable
fun LogView(
    tool: LogTool
) {
    val logList = remember {
        tool.logs
    }
    val logState = rememberLazyListState()
    val logAdapter = rememberScrollbarAdapter(
        scrollState = logState // TextBox height + Spacer height
    )
    val (showLogView, showLogViewSet) = remember {
        mutableStateOf(false)
    }

    val logViewHeight by remember(showLogView) {
        mutableStateOf(if (showLogView) 200 else 40)
    }

    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.align(Alignment.CenterStart)) {
            TextButton(
                onClick = {
                    showLogViewSet(!showLogView)
                }, modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Image(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = if (showLogView) {
                        Icon.Outlined.KeyboardArrowDown
                    } else {
                        Icon.Outlined.KeyboardArrowUp
                    },
                    contentDescription = "展开/收起日志",
                    colorFilter = ColorFilter.tint(contentColorFor(MaterialTheme.colors.background)),
                )
                Text(
                    text = if (showLogView) {
                        "收起日志"
                    } else {
                        "展开日志"
                    }
                )
            }
        }
        Column(Modifier.align(Alignment.CenterEnd)) {
            AnimatedVisibility(logList.isNotEmpty()) {
                TextButton(onClick = {
                    logList.clear()
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("清空日志")
                }
            }
        }
    }
    AnimatedContent(
        targetState = logViewHeight,
        transitionSpec = {
            if (targetState > initialState) {
                slideInVertically(
                    animationSpec = tween(200),
                    initialOffsetY = { it - initialState }
                ) togetherWith slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { -initialState }
                )
            } else {
                slideInVertically(
                    animationSpec = tween(200),
                    initialOffsetY = { targetState - initialState }
                ) togetherWith slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { initialState }
                )
            }
        }
    ) { targetState ->
        LogListView(
            modifier = Modifier.height(targetState.dp).fillMaxWidth(),
            logList = logList,
            state = logState,
            adapter = logAdapter,
        )
    }
}

