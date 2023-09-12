package com.virogu.pager

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virogu.bean.AdbDevice
import com.virogu.bean.ProcessInfo
import com.virogu.pager.view.OptionButton
import com.virogu.pager.view.SelectDeviceView
import com.virogu.pager.view.TipsView
import com.virogu.tools.Tools
import com.virogu.tools.process.DeviceProcessTool
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import theme.*

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:22:21
 **/
@Composable
fun DeviceProcessPager(
    tools: Tools,
) {
    val processTool = tools.processTool
    val currentDevice by tools.deviceConnectTool.currentSelectedDevice.collectAsState()

    val listState = rememberLazyListState()
    val scrollAdapter = rememberScrollbarAdapter(listState)

    val sortBy: MutableState<ProcessInfo.SortBy> = remember {
        mutableStateOf(ProcessInfo.SortBy.NAME)
    }

    val sortDesc = remember {
        mutableStateOf(false)
    }
    var processes by remember {
        mutableStateOf(emptyList<ProcessInfo>())
    }
    var currentSelect: ProcessInfo? by remember(currentDevice) {
        mutableStateOf(null)
    }
    LaunchedEffect(sortBy.value, sortDesc.value) {
        processTool.processListFlow.onEach { list ->
            processes = list.run {
                sortBy.value.sort(this, sortDesc.value)
            }
            if (currentSelect != null && list.find { it.pid == currentSelect?.pid } == null) {
                currentSelect = null
            }
        }.launchIn(this)
    }
    DisposableEffect("enter") {
        processTool.active()
        onDispose {
            processTool.pause()
        }
    }

    Box {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectDeviceView(
                Modifier.textFieldHeight().align(Alignment.CenterHorizontally).padding(horizontal = 16.dp),
                currentDevice, tools
            )
            ToolBarView(tools.processTool, currentDevice, currentSelect) {
                currentSelect = it
            }
            ProcessItemTitle(sortBy, sortDesc)
            Row {
                LazyColumn(
                    Modifier.fillMaxHeight().weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    processes.forEach {
                        item {
                            ProcessItemView(it, currentSelect) {
                                currentSelect = it
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                VerticalScrollbar(
                    modifier = Modifier,
                    adapter = scrollAdapter,
                    reverseLayout = false,
                )
            }
        }
        TipsView(Modifier.align(Alignment.BottomCenter), processTool.tipsFlow)
    }
}

@Composable
private fun ToolBarView(
    processTool: DeviceProcessTool,
    currentDevice: AdbDevice?,
    currentSelect: ProcessInfo?,
    selectProcess: (ProcessInfo?) -> Unit,
) {
    val deviceConnected = currentDevice?.isOnline == true
    val isBusy by processTool.isBusy.collectAsState()

    Box(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(35.dp)) {
        Row(Modifier.align(Alignment.CenterStart), Arrangement.spacedBy(8.dp)) {
            OptionButton(
                "停止进程\nkill",
                enable = deviceConnected && currentSelect != null,
                painter = Icon.Outlined.Dangerous,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = materialColors.error
                ),
            ) label@{
                currentSelect ?: return@label
                processTool.killProcess(currentSelect)
                selectProcess(null)
            }

            OptionButton(
                "强行停止程序\nforce stop",
                enable = deviceConnected && currentSelect != null,
                painter = Icon.Outlined.Stop,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = materialColors.error
                ),
            ) label@{
                currentSelect ?: return@label
                processTool.forceStopProcess(currentSelect)
                selectProcess(null)
            }

            OptionButton(
                "刷新",
                enable = deviceConnected,
                painter = Icon.Outlined.Sync
            ) {
                processTool.refresh()
            }
        }
        if (isBusy) {
            val infiniteTransition by rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            Icon(
                modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).rotate(infiniteTransition),
                painter = Icon.Outlined.ClockLoader,
                contentDescription = "运行状态"
            )
        }
    }
}

@Composable
private fun ProcessItemTitle(
    sortBy: MutableState<ProcessInfo.SortBy>,
    sortDesc: MutableState<Boolean>
) {
    val borderColor = materialColors.onSurface.copy(alpha = 0.5f)
    Card(modifier = Modifier.height(40.dp), elevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val boxModifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically)
            val icModifier = boxModifier.size(24.dp)
            val tabModifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp)
            val spacerModifier = Modifier.fillMaxHeight().width(1.dp).background(borderColor)
            val imageVector = remember(sortDesc.value) {
                if (sortDesc.value) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.Filled.KeyboardArrowUp
                }
            }
            Box(boxModifier.weight(6f).clickable {
                sortBy.value = ProcessInfo.SortBy.NAME
                sortDesc.value = !sortDesc.value
            }) {
                Row(tabModifier.align(Alignment.CenterStart)) {
                    if (sortBy.value.tag == ProcessInfo.SortBy.NAME.tag) {
                        Icon(modifier = icModifier, imageVector = imageVector, contentDescription = "排序")
                    } else {
                        Spacer(icModifier)
                    }
                    Text("Name", Modifier.align(Alignment.CenterVertically))
                }
            }
            Spacer(spacerModifier)
            Box(boxModifier.weight(2f).clickable {
                sortBy.value = ProcessInfo.SortBy.PID
                sortDesc.value = !sortDesc.value
            }) {
                Row(tabModifier.align(Alignment.Center)) {
                    if (sortBy.value.tag == ProcessInfo.SortBy.PID.tag) {
                        Icon(modifier = icModifier, imageVector = imageVector, contentDescription = "排序")
                    } else {
                        Spacer(icModifier)
                    }
                    Text("PID", Modifier.align(Alignment.CenterVertically))
                }
            }
            Spacer(spacerModifier)
            Box(boxModifier.weight(2f)) {
                Row(tabModifier.align(Alignment.Center)) {
                    Spacer(icModifier)
                    Text("ABI", Modifier.align(Alignment.CenterVertically))
                }
            }
            Spacer(spacerModifier)
            Box(boxModifier.weight(2f)) {
                Row(tabModifier.align(Alignment.Center)) {
                    Spacer(icModifier)
                    Text("User", Modifier.align(Alignment.CenterVertically))
                }
            }
            //Spacer(spacerModifier)
            //Box(boxModifier.weight(2f)) {
            //    Row(tabModifier.align(Alignment.Center)) {
            //        Spacer(icModifier)
            //        Text("RSS", Modifier.align(Alignment.CenterVertically))
            //    }
            //}
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ProcessItemView(
    processInfo: ProcessInfo,
    currentSelect: ProcessInfo?,
    selectProcess: (ProcessInfo?) -> Unit,
) {
    val selected = remember(processInfo, currentSelect) {
        mutableStateOf(currentSelect?.pid == processInfo.pid)
    }
    val primaryColor = materialColors.primary.copy(alpha = 0.5f)
    var mouseEnter by remember { mutableStateOf(false) }
    val backgroundColor by remember(selected.value, mouseEnter) {
        val c = if (selected.value) {
            primaryColor.copy(alpha = 0.5f)
        } else if (mouseEnter) {
            primaryColor.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        }
        mutableStateOf(c)
    }

    Card(modifier = Modifier.height(40.dp).onPointerEvent(PointerEventType.Enter) {
        mouseEnter = true
    }.onPointerEvent(PointerEventType.Exit) {
        mouseEnter = false
    }.onPointerEvent(PointerEventType.Press) {
        selectProcess(processInfo)
    }, backgroundColor = backgroundColor, elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 4.dp)
            val iconModifier = modifier.size(20.dp)
            Row(
                modifier = modifier.weight(6f), horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = iconModifier,
                    painter = Icon.Outlined.SmartPhone,
                    contentDescription = "icon smartphone"
                )
                TooltipArea(
                    tooltip = {
                        Card(elevation = 4.dp) {
                            Text(text = processInfo.processName, modifier = Modifier.padding(8.dp))
                        }
                    },
                    modifier = modifier.weight(1f),
                    delayMillis = 500,
                ) {
                    Text(
                        text = processInfo.processName,
                        modifier = modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = processInfo.pid,
                modifier = modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Text(
                text = processInfo.abi,
                modifier = modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Text(
                text = processInfo.user,
                modifier = modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            //Text(
            //    text = processInfo.lastRss,
            //    modifier = modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            //    textAlign = TextAlign.End,
            //)
        }
    }
}