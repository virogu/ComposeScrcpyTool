@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.virogu.tools.Tools
import theme.materialColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPager(
    window: ComposeWindow,
    windowState: WindowState,
    pagerController: PagerNavController<Pager>,
    tools: Tools
) {
    val currentPager = pagerController.currentPager.collectAsState()

    val fileListState = rememberLazyListState()
    val deviceConnectListState = rememberLazyListState()

    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.wrapContentWidth().fillMaxHeight().padding(8.dp),
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
        Spacer(Modifier.fillMaxHeight().width(1.dp).background(materialColors.onSurface.copy(alpha = 0.3f)))
        when (val pager = currentPager.value) {
            Pager.DeviceConnection -> DeviceConnectView(
                window,
                windowState,
                tools,
                deviceConnectListState
            )

            Pager.DeviceExplorer -> FileExplorerPager(tools, fileListState)
            Pager.DeviceProcess -> Text(pager.title)
        }
    }
}