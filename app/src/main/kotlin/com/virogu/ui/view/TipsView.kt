package com.virogu.ui.view

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import theme.ContentCopy
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
    val clipboardManager = LocalClipboardManager.current

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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Info, "info")
                Spacer(Modifier.size(4.dp))
                Text(tips, Modifier.weight(1f))
                //TextButton(onClick = {
                //    clipboardManager.setText(AnnotatedString(tips))
                //}, modifier = Modifier.align(Alignment.CenterVertically)) {
                //    Text("复制")
                //}
                //TextButton(onClick = {
                //    showTips = false
                //}, modifier = Modifier.align(Alignment.CenterVertically)) {
                //    Text("清除")
                //}
                IconButton({
                    clipboardManager.setText(AnnotatedString(tips))
                }) {
                    Icon(Icon.Outlined.ContentCopy, "复制")
                }
                IconButton({
                    showTips = false
                }) {
                    Icon(Icons.Default.Close, "关闭")
                }
                //Button(
                //    onClick = { showTips = false },
                //    modifier = Modifier.size(35.dp),
                //    shape = CircleShape,
                //    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                //    contentPadding = PaddingValues(4.dp),
                //    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
                //) {
                //    Icon(Icons.Default.Close, "关闭")
                //}
            }
        }
    }
}