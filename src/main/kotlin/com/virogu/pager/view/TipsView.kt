package com.virogu.pager.view

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import theme.materialColors

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:26:55
 **/

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TipsView(
    modifier: Modifier,
    tipsFlow: SharedFlow<String>
) {
    var tips by remember {
        mutableStateOf("")
    }
    var showTips by remember {
        mutableStateOf(false)
    }
    var mouseEnter by remember { mutableStateOf(false) }

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
                modifier = Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Filled.Info, "info")
                Text(tips, Modifier.weight(1f))
                Button(
                    onClick = { showTips = false },
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    contentPadding = PaddingValues(4.dp),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Close, "关闭")
                }
            }
        }
    }
}