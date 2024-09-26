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
    StatusBar("通知栏", { Icon.Outlined.KeyboardDoubleArrowDown }),
    ScreenShot("截图", { Icon.Outlined.Screenshot }),

    //OpenScreen("打开屏幕", { Icon.Outlined.Preview }, ),
    //CloseScreen("关闭屏幕", { Icon.Outlined.PreviewOff }, ),
    PowerButton("电源键", { Icon.Outlined.PowerSetting }),
    VolumePlus("音量+", { Icon.Outlined.VolumeUp }),
    VolumeReduce("音量-", { Icon.Outlined.VolumeDown }),
    TaskManagement("切换应用", { Icon.Outlined.SelectWindow }),
    Menu("菜单", { Icons.Outlined.Menu.painter() }),
    Home("主界面", { Icons.Outlined.Home.painter() }),
    Back("返回", { Icons.AutoMirrored.Outlined.ArrowBack.painter() }),
}