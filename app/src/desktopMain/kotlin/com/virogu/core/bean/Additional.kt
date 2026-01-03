/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.bean

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import theme.Icon

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
    Menu("菜单", { Icon.Outlined.Menu }),
    Home("主界面", { Icon.Outlined.Home }),
    Back("返回", { Icon.Outlined.ArrowBack }),
}