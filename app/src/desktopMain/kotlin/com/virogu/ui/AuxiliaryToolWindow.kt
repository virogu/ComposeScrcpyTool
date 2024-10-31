package com.virogu.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virogu.core.bean.Additional
import com.virogu.core.tool.Tools
import com.virogu.core.viewmodel.AuxiliaryViewModel
import theme.*

/**
 * Created by Virogu
 * Date 2023/11/13 下午 4:20:13
 **/

@Composable
fun AuxiliaryToolWindow(
    show: MutableState<Boolean>,
    tools: Tools,
) {
    if (!show.value) {
        return
    }
    val icon = Icon.Logo
    val defaultHeight = (50 + Additional.entries.size * (45 + 8)).coerceAtMost(800)
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = DpSize(60.dp, defaultHeight.dp),
        position = WindowPosition.Aligned(Alignment.CenterEnd),
    )
    val alwaysOnTop = remember {
        mutableStateOf(true)
    }

    Window(
        onCloseRequest = {
            show.value = false
        },
        title = "",
        state = state,
        undecorated = true,
        alwaysOnTop = alwaysOnTop.value,
        icon = icon,
    ) {
        MainTheme {
            Column(Modifier.fillMaxSize()) {
                ToolBar(alwaysOnTop, show)
                ToolsView(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WindowScope.ToolBar(
    alwaysOnTop: MutableState<Boolean>,
    show: MutableState<Boolean>
) {
    Column {
        val modifier = Modifier.fillMaxWidth().height(30.dp).background(Color.Black.copy(0.2f))
        WindowDraggableArea {
            Box(modifier) {
                TooltipArea(
                    modifier = Modifier.align(Alignment.Center),
                    tooltip = {
                        Card(elevation = 4.dp) {
                            Text(
                                text = if (alwaysOnTop.value) "取消置顶" else "置顶",
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    },
                    delayMillis = 500, // in milliseconds
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp).align(Alignment.Center).onClick(
                            onDoubleClick = {
                                show.value = false
                            }, onClick = {
                                alwaysOnTop.value = !alwaysOnTop.value
                            }
                        ),
                        painter = if (alwaysOnTop.value) Icon.Outlined.Keep else Icon.Outlined.KeepOf,
                        contentDescription = "置顶",
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolsView(
    modifier: Modifier = Modifier,
    viewModel: AuxiliaryViewModel = viewModel { AuxiliaryViewModel() },
) {
    val isBusy by viewModel.isBusy.collectAsState()
    val currentDevice by viewModel.selectedOnlineDevice.collectAsState()

    val list = remember {
        Additional.entries.toTypedArray()
    }
    val onClick: (Additional) -> Unit by rememberUpdatedState label@{
        if (currentDevice == null || isBusy) {
            return@label
        }
        viewModel.exec(it)
    }
    LazyColumn(
        modifier = modifier.wrapContentWidth().fillMaxHeight().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list) {
            TooltipArea(
                tooltip = {
                    Card(elevation = 4.dp) {
                        Text(text = it.title, modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 500, // in milliseconds
            ) {
                Button(
                    onClick = { onClick(it) },
                    modifier = Modifier.size(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent
                    ),
                    elevation = null,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = it.imgPainter(),
                        contentDescription = it.title,
                    )
                }
            }
        }
    }
}
