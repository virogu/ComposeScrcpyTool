@file:Suppress("unused", "FunctionName")

package com.virogu.pager

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.virogu.bean.Configs
import com.virogu.pager.view.LogListView
import com.virogu.pager.view.animateBorderStrokeAsState
import com.virogu.tools.Tools
import com.virogu.tools.log.LogTool
import logger

/**
 * @author Virogu
 * @since 2022-08-31 15:40
 **/
@Composable
fun MainView(window: ComposeWindow, windowState: WindowState, tools: Tools) {
    //val stateHorizontal = rememberScrollState(0)
    //val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyColumn(
            Modifier.weight(5f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConnectDeviceView(windowState, tools)
            }
            item {
                DeviceListView(windowState, tools)
            }
            item {
                DeviceView(tools)
            }
            item {
                ScrcpyView(tools)
            }
        }
        LogView(tools.logTool)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConnectDeviceView(
    windowState: WindowState,
    tools: Tools
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val configTool = tools.configTool
        val connectTool = tools.deviceConnectTool
        val isBusy = connectTool.isBusy.collectAsState()
        val history = configTool.historyDeviceFlow.collectAsState()

        val ip = remember {
            mutableStateOf("")
        }
        val port = remember {
            mutableStateOf(5555)
        }

        //for drop menu
        val expanded = remember { mutableStateOf(false) }
        val dropMenuWidth = remember {
            mutableStateOf(0.dp)
        }
        val dropMenuOffset = remember {
            mutableStateOf(0.dp)
        }

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
                logger.info("IP地址[$ipString]无效")
                return@label
            }
            configTool.updateLastConnect(
                Configs.HistoryDevice(
                    System.currentTimeMillis(),
                    ip.value,
                    port.value
                )
            )
            connectTool.connect(ip.value, port.value)
        }

        val ipModifier = Modifier.onKeyEvent { event ->
            if (event.key == Key.Enter) {
                connectAction()
                true
            } else {
                false
            }
        }
        Text("无线连接", modifier = Modifier.width(80.dp).align(Alignment.CenterVertically).onPlaced {
            dropMenuOffset.value = (it.size.width + 8).dp
        })
        Row(
            modifier = Modifier.weight(1f).onPlaced {
                dropMenuWidth.value = it.size.width.dp
            },
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
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = contentColorFor(MaterialTheme.colors.background))
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
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = contentColorFor(MaterialTheme.colors.background))
            )
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
            },
            modifier = Modifier.width(dropMenuWidth.value)
                .heightIn(0.dp, (windowState.size.height.value * 0.5).dp),
            offset = DpOffset(dropMenuOffset.value, 0.dp)
        ) {
            if (history.value.isNotEmpty()) {
                Column(modifier = Modifier.clickable {
                    configTool.clearHistoryConnect()
                }) {
                    Text(text = "清空连接记录", modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 10.dp))
                }
            }
            history.value.forEach { device ->
                Row(modifier = Modifier.clickable {
                    ip.value = device.ip
                    port.value = device.port
                    expanded.value = !expanded.value
                }) {
                    Text(text = device.showName, modifier = Modifier.weight(1f).padding(16.dp, 10.dp, 16.dp, 10.dp))
                    Icon(
                        painter = painterResource(
                            if (device.tagged) {
                                "icons/ic_tagged.svg"
                            } else {
                                "icons/ic_tagging.svg"
                            }
                        ),
                        contentDescription = if (device.tagged) "取消置顶" else "置顶",
                        modifier = Modifier.size(40.dp).clickable {
                            configTool.updateLastConnectTagged(device, !device.tagged)
                        }.padding(12.dp).align(Alignment.CenterVertically),
                        tint = contentColorFor(MaterialTheme.colors.background)
                    )
                    Icon(
                        Icons.Default.Close,
                        "",
                        modifier = Modifier.size(40.dp).clickable {
                            configTool.removeLastConnect(device)
                        }.padding(10.dp).align(Alignment.CenterVertically),
                        tint = contentColorFor(MaterialTheme.colors.background)
                    )
                }
            }
        }
        Button(
            onClick = {
                expanded.value = !expanded.value
            },
            modifier = Modifier.align(Alignment.CenterVertically),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 0, 0, alpha = 0)),
            contentPadding = PaddingValues(4.dp),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        ) {
            Icon(Icons.Default.ArrowDropDown, "", tint = contentColorFor(MaterialTheme.colors.background))
        }
        TextButton(
            onClick = connectAction,
            enabled = !isBusy.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("无线连接")
        }
    }

}

@Composable
fun DeviceListView(
    windowState: WindowState,
    tools: Tools,
) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        val connectTool = tools.deviceConnectTool
        val isBusy = connectTool.isBusy.collectAsState()
        val current = connectTool.currentSelectedDevice.collectAsState()
        val devices = connectTool.connectedDevice.collectAsState()

        val expanded = remember { mutableStateOf(false) }
        val borderStroke = animateBorderStrokeAsState()

        val dropMenuWidth = remember {
            mutableStateOf(0.dp)
        }
        val dropMenuOffset = remember {
            mutableStateOf(0.dp)
        }
        Text("设备列表", modifier = Modifier.width(80.dp).align(Alignment.CenterVertically).onPlaced {
            dropMenuOffset.value = (it.size.width + 8).dp
        })
        Box(
            modifier = Modifier.weight(1f).border(
                borderStroke.value,
                TextFieldDefaults.OutlinedTextFieldShape
            ).defaultMinSize(
                minWidth = TextFieldDefaults.MinWidth,
                minHeight = TextFieldDefaults.MinHeight
            ).clickable {
                expanded.value = true
            }.onPlaced {
                dropMenuWidth.value = it.size.width.dp
            },
        ) {
            SelectionContainer {
                Text(
                    text = current.value?.showName.orEmpty(),
                    modifier = Modifier.align(Alignment.CenterStart).padding(16.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
            },
            modifier = Modifier.width(dropMenuWidth.value)
                .heightIn(0.dp, (windowState.size.height.value * 0.5).dp),
            offset = DpOffset(dropMenuOffset.value, 0.dp)
        ) {
            devices.value.forEach {
                Column(modifier = Modifier.clickable {
                    connectTool.selectDevice(it)
                    expanded.value = false
                }) {
                    Text(text = it.showName, modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 10.dp))
                    //Box(modifier = Modifier.fillMaxWidth().padding(16.dp).height(0.5.dp).background(Color.LightGray))
                }
            }
        }
        Button(
            onClick = {
                expanded.value = !expanded.value
            },
            modifier = Modifier.align(Alignment.CenterVertically),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 0, 0, alpha = 0)),
            contentPadding = PaddingValues(4.dp),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        ) {
            Icon(Icons.Default.ArrowDropDown, "", tint = contentColorFor(MaterialTheme.colors.background))
        }
        TextButton(
            onClick = {
                connectTool.refresh()
            },
            enabled = !isBusy.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("刷新设备")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeviceView(tools: Tools) {
    val connectTool = tools.deviceConnectTool
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
            modifier = Modifier.onKeyEvent { event ->
                if (event.key == Key.Enter) {
                    updateDescAction()
                    true
                } else {
                    false
                }
            }.weight(1f),
            singleLine = true,
            onValueChange = {
                desc.value = it.take(20)
            },
            placeholder = { Text("Phone") }
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
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("断开连接")
        }
        TextButton(
            onClick = {
                connectTool.disconnectAll()
            },
            enabled = !isBusy.value,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("全部断开")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
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
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.align(Alignment.CenterStart)) {
            TextButton(
                onClick = {
                    showLogViewSet(!showLogView)
                }, modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Image(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    imageVector = if (showLogView) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.Filled.KeyboardArrowUp
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
        targetState = showLogView,
        transitionSpec = {
            if (targetState) {
                slideInVertically(initialOffsetY = { it }) + expandVertically() with
                        slideOutVertically(targetOffsetY = { it }) + shrinkVertically()
            } else {
                slideInVertically(initialOffsetY = { -it }) + expandVertically() with
                        slideOutVertically(targetOffsetY = { -it }) + shrinkVertically()
            }
        }
    ) { targetState ->
        LogListView(
            modifier = Modifier.height(if (targetState) 200.dp else 60.dp),
            logList = logList,
            state = logState,
            adapter = logAdapter,
        )
    }
}
