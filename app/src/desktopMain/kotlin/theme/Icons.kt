/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import composescrcpytool.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun ImageVector.painter() = rememberVectorPainter(this)

object Icon {
    val Logo: Painter
        @Composable
        get() = painterResource(Res.drawable.logo)

    object Outlined {
        val AdminPanelSettings: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_admin_panel_settings)

        val ArrowBack: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_arrow_back)

        val ArrowForward: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_arrow_forward)

        val ClockLoader: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_clock_loader)

        val Close: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_close)

        val Construction: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_construction)

        val ContentCopy: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_content_copy)

        val CPU: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_memory)

        val Dangerous: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_dangerous)

        val DesktopAndPhone: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_device_connection)

        val Download: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_download)

        val DragPan: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_drag_pan)

        val FileDocument: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_file)

        val FileFolder: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_folder)

        val FileFolderOpen: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_folder_open)

        val FileLink: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_file_link)

        val FileNewFile: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_new_file)

        val FileNewFolder: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_new_folder)

        val FileUnknown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_unknown_file)

        val FilterNone: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_filter_none)

        val Home: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_home)

        val Info: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_info)

        val Keep: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keep)

        val KeepOf: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keep_off)

        val KeyboardArrowDown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keyboard_arrow_down)

        val KeyboardArrowLeft: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keyboard_arrow_left)

        val KeyboardArrowRight: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keyboard_arrow_right)

        val KeyboardArrowUp: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keyboard_arrow_up)

        val KeyboardDoubleArrowDown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keyboard_double_arrow_down)

        val Menu: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_menu)

        val MoreHorizon: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_more_horiz)

        val PowerSetting: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_power_settings_new)

        val Preview: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_preview)

        val PreviewOff: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_preview_off)

        val Screenshot: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_screenshot)

        val SelectWindow: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_select_window)

        val SmartPhone: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_smartphone)

        val Star: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_star)

        val Stop: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_stop)

        val Sync: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_sync)

        val TrashCan: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_delete)

        val Upload: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_upload)

        val VolumeDown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_volume_down_alt)

        val VolumeUp: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_volume_up)
    }

    object Filled {
        val Star: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_star_fill)
        val FileFolder: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_folder_fill)
        val FileFolderOpen: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_folder_open_fill)
    }
}
