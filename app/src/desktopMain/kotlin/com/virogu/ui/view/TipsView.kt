/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.ui.view

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.setText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import theme.Icon
import theme.materialColors

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:26:55
 **/

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TipsView(
    modifier: Modifier,
    tipsFlow: Flow<String>
) {
    var tips by remember {
        mutableStateOf("")
    }
    var showTips by remember {
        mutableStateOf(false)
    }
    var mouseEnter by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val listAdapter = rememberScrollbarAdapter(scrollState = listState)
    LaunchedEffect(Unit) {
        tipsFlow.onEach {
            tips = it.trim()
            showTips = true
        }.launchIn(this)
    }

    LaunchedEffect(showTips, mouseEnter) {
        if (!mouseEnter && showTips) {
            delay(5000)
            showTips = false
        }
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = showTips,
        enter = fadeIn() + expandIn(initialSize = { IntSize(it.width, 0) }),
        exit = shrinkOut(targetSize = { IntSize(it.width, 0) }) + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .onPointerEvent(PointerEventType.Enter) { mouseEnter = true }
                .onPointerEvent(PointerEventType.Exit) { mouseEnter = false },
            border = if (mouseEnter) BorderStroke(1.dp, materialColors.primary) else null,
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight().heightIn(60.dp, 200.dp)
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icon.Outlined.Info, "info")
                Spacer(Modifier.size(4.dp))
                var lazyColumnHeight by remember { mutableStateOf(0) }
                LazyColumn(Modifier.weight(1f).onGloballyPositioned {
                    lazyColumnHeight = it.size.height
                }, state = listState) {
                    item {
                        SelectionContainer {
                            Text(tips)
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.width(8.dp).height(lazyColumnHeight.dp),
                    adapter = listAdapter
                )
                IconButton({
                    scope.launch {
                        clipboard.setText(tips)
                    }
                }) {
                    Icon(Icon.Outlined.ContentCopy, "复制")
                }
                IconButton({
                    showTips = false
                }) {
                    Icon(Icon.Outlined.Close, "关闭")
                }
            }

        }
    }
}