package com.virogu.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.virogu.bean.Auxiliary
import com.virogu.tools.Tools
import theme.MainTheme

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
    val icon = painterResource("logo.svg")
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = DpSize(60.dp, 500.dp),
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
                ToolBar(alwaysOnTop)
                ToolsView(tools, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun WindowScope.ToolBar(alwaysOnTop: MutableState<Boolean>) = WindowDraggableArea {
    Box(Modifier.fillMaxWidth().height(30.dp).background(Color.Black.copy(0.1f))) {
//        Row(Modifier.align(Alignment.CenterEnd)) {
//            Button(
//                onClick = {
//                },
//                modifier = Modifier.size(30.dp),
//                shape = RoundedCornerShape(0.dp),
//                colors = ButtonDefaults.buttonColors(
//                    backgroundColor = Color.Transparent
//                ),
//                elevation = null,
//                contentPadding = PaddingValues(0.dp)
//            ) {
//                Icon(
//                    modifier = Modifier.size(20.dp),
//                    imageVector = Icons.Filled.Close,
//                    contentDescription = "关闭",
//                )
//            }
//        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolsView(
    tools: Tools,
    modifier: Modifier = Modifier,
) {
    val auxiliaryTool = tools.auxiliaryTool
    val isBusy by auxiliaryTool.isBusy.collectAsState()
    val currentDevice by auxiliaryTool.selectedOnlineDevice.collectAsState()
    val enable by remember(currentDevice, isBusy) {
        mutableStateOf(currentDevice != null && !isBusy)
    }

    val list = remember {
        Auxiliary.entries.toTypedArray()
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
                    onClick = {
                        auxiliaryTool.exec(it.opt)
                    },
                    enabled = enable,
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
