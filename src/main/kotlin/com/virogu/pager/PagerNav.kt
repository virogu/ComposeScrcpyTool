package com.virogu.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import theme.CPU
import theme.DesktopAndPhone
import theme.FileFolderOpen
import theme.Icon

sealed class Pager(
    val title: String,
    val imgPainter: @Composable () -> Painter
) {
    data object DeviceConnection : Pager(
        title = "设备连接", imgPainter = {
            Icon.Outlined.DesktopAndPhone
        }
    )

    data object DeviceExplorer : Pager(
        title = "文件管理", imgPainter = {
            Icon.Outlined.FileFolderOpen
        }
    )

    data object DeviceProcess : Pager(
        title = "进程管理", imgPainter = {
            Icon.Outlined.CPU
        }
    )
}

interface PagerNavController<T> {
    val defaultPager: T
    val pagers: List<T>
    val currentPager: StateFlow<T>

    fun navigate(pager: T)
}

@Composable
fun <S> rememberPagerController(
    pagers: List<S>,
    defaultPager: S
): State<PagerNavController<S>> = remember {
    mutableStateOf(object : PagerNavController<S> {
        override val defaultPager: S = defaultPager
        override val pagers: List<S> = pagers
        override val currentPager: MutableStateFlow<S> = MutableStateFlow(defaultPager)

        override fun navigate(pager: S) {
            currentPager.tryEmit(pager)
        }
    })
}