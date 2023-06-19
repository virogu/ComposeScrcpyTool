package com.virogu.bean

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Created by Virogu
 * Date 2023/11/13 下午 6:01:22
 **/

enum class Auxiliary(
    val title: String,
    val imgPainter: @Composable () -> Painter,
    val opt: String,
) {
    //TODO
    //下拉通知栏
    //打开屏幕
    //关闭屏幕
    //电源
    //音量+
    //音量-
    //切换应用/后台任务管理
    //菜单
    //主界面
    //返回
}