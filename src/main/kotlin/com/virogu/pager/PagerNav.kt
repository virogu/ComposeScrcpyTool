package com.virogu.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class Pager(
    val title: String,
    val imgPainter: @Composable () -> Painter
) {
    object DeviceConnection : Pager(
        title = "设备连接", imgPainter = {
            painterResource("icons/ic_device_connection.svg")
        }
    )

    object DeviceExplorer : Pager(
        title = "文件管理", imgPainter = {
            painterResource("icons/ic_folder.svg")
        }
    )

    object DeviceProcess : Pager(
        title = "进程管理", imgPainter = {
            painterResource("icons/ic_process.svg")
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