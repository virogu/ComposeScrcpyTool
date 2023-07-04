package com.virogu.bean

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import theme.*

/**
 * Created by Virogu
 * Date 2023/11/13 下午 6:01:22
 **/

enum class Auxiliary(
    val title: String,
    val imgPainter: @Composable () -> Painter,
    val command: Array<String>,
) {
    //下拉通知栏
    StatusBar("通知栏", { Icon.Outlined.KeyboardDoubleArrowDown }, arrayOf("shell", "input keyevent 83")),

    //打开屏幕
    //OpenScreen("打开屏幕", { Icon.Outlined.Preview }, arrayOf("shell", "input keyevent 224")),

    //关闭屏幕
    //CloseScreen("关闭屏幕", { Icon.Outlined.PreviewOff }, arrayOf("shell", "input keyevent 223")),

    //电源
    PowerButton("电源键", { Icon.Outlined.PowerSetting }, arrayOf("shell", "input keyevent 26")),

    //音量+
    VolumePlus("音量+", { Icon.Outlined.VolumeUp }, arrayOf("shell", "input keyevent 24")),

    //音量-
    VolumeReduce("音量-", { Icon.Outlined.VolumeDown }, arrayOf("shell", "input keyevent 25")),

    //切换应用/后台任务管理
    TaskManagement("切换应用", { Icon.Outlined.SelectWindow }, arrayOf("shell", "input keyevent 187")),

    //菜单
    Menu("菜单", { Icons.Outlined.Menu.painter() }, arrayOf("shell", "input keyevent 82")),

    //主界面
    Home("主界面", { Icons.Outlined.Home.painter() }, arrayOf("shell", "input keyevent 3")),

    //返回
    Back("返回", { Icons.Outlined.ArrowBack.painter() }, arrayOf("shell", "input keyevent 4")),
}