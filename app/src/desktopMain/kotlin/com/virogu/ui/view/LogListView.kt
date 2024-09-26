@file:Suppress("FunctionName")

package com.virogu.ui.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import java.text.SimpleDateFormat

/**
 * @author Virogu
 * @since 2023-07-14 10:47
 **/

@Composable
fun LogListView(
    modifier: Modifier = Modifier.fillMaxSize(),
    logList: SnapshotStateList<ILoggingEvent>,
    state: LazyListState,
    adapter: ScrollbarAdapter,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    //val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(logList.size) {
        state.scrollListToEnd()
    }
    Box(modifier = modifier) {
        SelectionContainer {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                state = state,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
            ) {
                items(logList) { log ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = timeStampColor)) {
                                append(log.logTimeStamp)
                            }
                            append(" ")
                            withStyle(SpanStyle(color = log.color)) {
                                append(log.logMsg)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = adapter,
            reverseLayout = reverseLayout,
        )
    }
}

private suspend fun LazyListState.scrollListToEnd(
    //当前列表停留位置距离底部不超过此数量时就自动滑动到底部
    offset: Int = 10
) {
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val total = layoutInfo.totalItemsCount
    visibleItemsInfo.getOrNull(visibleItemsInfo.lastIndex)?.also { layout ->
        if (layout.index in (total - offset)..total) {
            //println("${layout.index} scrollToItem: $total layout.offset: ${layout.offset}")
            try {
                scrollToItem(total, 0)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            //println("${layout.index} not scrollToItem: $total")
        }
    }
}

private val ILoggingEvent.color
    get() = when (this.level) {
        //Level.DEBUG -> Color.Unspecified
        Level.DEBUG -> Color(57, 147, 212)
        Level.INFO -> Color(73, 156, 84)
        Level.WARN -> Color(140, 102, 48)
        Level.ERROR -> Color.Red
        else -> Color.Unspecified
    }

private val simpleDateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS")

private val timeStampColor = Color.Unspecified

private val ILoggingEvent.logTimeStamp
    get() = simpleDateFormat.format(this.timeStamp)

private val ILoggingEvent.logMsg
    get() = this.formattedMessage

private val ILoggingEvent.logFormatMsg
    get() = "${simpleDateFormat.format(this.timeStamp)} ${this.formattedMessage}"