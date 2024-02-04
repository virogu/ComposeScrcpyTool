package com.virogu.core.bean

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import theme.*

/**
 * Created by Virogu
 * Date 2023/11/13 下午 6:01:22
 **/

enum class Additional(
    val title: String,
    val imgPainter: @Composable () -> Painter,
) {
    //下拉通知栏
    StatusBar("通知栏", { Icon.Outlined.KeyboardDoubleArrowDown }),

    //打开屏幕
    //OpenScreen("打开屏幕", { Icon.Outlined.Preview }, ),

    //关闭屏幕
    //CloseScreen("关闭屏幕", { Icon.Outlined.PreviewOff }, ),

    //电源
    PowerButton("电源键", { Icon.Outlined.PowerSetting }),

    //音量+
    VolumePlus("音量+", { Icon.Outlined.VolumeUp }),

    //音量-
    VolumeReduce("音量-", { Icon.Outlined.VolumeDown }),

    //切换应用/后台任务管理
    TaskManagement("切换应用", { Icon.Outlined.SelectWindow }),

    //菜单
    Menu("菜单", { Icons.Outlined.Menu.painter() }),

    //主界面
    Home("主界面", { Icons.Outlined.Home.painter() }),

    //返回
    Back("返回", { Icons.AutoMirrored.Outlined.ArrowBack.painter() }),
}