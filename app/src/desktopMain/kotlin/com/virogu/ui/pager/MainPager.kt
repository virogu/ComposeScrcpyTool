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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.virogu.core.tool.Tools
import com.virogu.ui.AuxiliaryToolWindow
import com.virogu.ui.Pager
import com.virogu.ui.PagerNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import theme.Icon
import theme.materialColors
import java.awt.SystemTray

@Composable
fun MainPager(
    trayState: TrayState,
    pagerController: PagerNavController<Pager>,
    tools: Tools
) {
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.wrapContentWidth().fillMaxHeight()) {
            TopMenuView(pagerController, modifier = Modifier.weight(1f))
            BottomMenuView(trayState, tools)
        }
        Spacer(Modifier.fillMaxHeight().width(1.dp).background(materialColors.onSurface.copy(alpha = 0.3f)))
        PagerContainerView(pagerController, tools)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopMenuView(
    pagerController: PagerNavController<Pager>,
    modifier: Modifier = Modifier,
) {
    val currentPager = pagerController.currentPager.collectAsState()
    LazyColumn(
        modifier = modifier.wrapContentWidth().fillMaxHeight().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pagerController.pagers) {
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
                        pagerController.navigate(it)
                    },
                    modifier = Modifier.size(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (currentPager.value == it) {
                            materialColors.primary.copy(alpha = 0.3f)
                        } else {
                            Color.Transparent
                        }
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

@Composable
private fun PagerContainerView(
    pagerController: PagerNavController<Pager>,
    tools: Tools
) {
    val currentPager = pagerController.currentPager.collectAsState()
    val fileListState = rememberLazyListState()
    val deviceConnectListState = rememberLazyListState()
    when (currentPager.value) {
        Pager.DeviceConnection -> DeviceConnectView(
            tools,
            deviceConnectListState
        )

        Pager.DeviceExplorer -> FileExplorerPager(tools, fileListState)
        Pager.DeviceProcess -> DeviceProcessPager(tools)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomMenuView(
    trayState: TrayState,
    tools: Tools,
    modifier: Modifier = Modifier
) {
    val showAuxiliaryTool = remember { mutableStateOf(false) }
    if (SystemTray.isSupported()) {
        LaunchedEffect(Unit) {
            tools.notification.onEach {
                trayState.sendNotification(Notification("ScrcpyTool", it, Notification.Type.Info))
            }.launchIn(this)
        }
    }
    Column(
        modifier = modifier.wrapContentWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TooltipArea(
            tooltip = { Card(elevation = 4.dp) { Text(text = "辅助工具", modifier = Modifier.padding(10.dp)) } },
            delayMillis = 500, // in milliseconds
        ) {
            Button(
                onClick = {
                    showAuxiliaryTool.value = !showAuxiliaryTool.value
                },
                modifier = Modifier.size(45.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (showAuxiliaryTool.value) {
                        materialColors.primary.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    }
                ),
                elevation = null,
                contentPadding = PaddingValues(8.dp)
            ) {
                Icons
                Icon(
                    modifier = Modifier.size(28.dp),
                    painter = Icon.Outlined.Construction,
                    contentDescription = "辅助工具",
                )
            }
        }
    }
    AuxiliaryToolWindow(showAuxiliaryTool, tools)
}