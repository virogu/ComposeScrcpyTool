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

        val ClockLoader: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_clock_loader)

        val Construction: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_construction)

        val Dangerous: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_dangerous)

        val TrashCan: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_delete)

        val DesktopAndPhone: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_device_connection)

        val Download: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_download)

        val FileDocument: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_file)

        val FileLink: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_file_link)

        val FileFolder: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_folder)

        val FileFolderOpen: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_folder_open)

        val FileNewFile: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_new_file)

        val FileNewFolder: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_new_folder)

        val FileUnknown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_unknown_file)

        val CPU: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_memory)

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

        val Upload: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_upload)

        val PowerSetting: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_power_settings_new)

        val VolumeDown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_volume_down_alt)

        val VolumeUp: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_volume_up)

        val Preview: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_preview)

        val PreviewOff: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_preview_off)

        val FilterNone: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_filter_none)

        val SelectWindow: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_select_window)

        val KeyboardDoubleArrowDown: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keyboard_double_arrow_down)

        val Screenshot: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_screenshot)

        val DragPan: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_drag_pan)

        val MoreHorizon: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_more_horiz)

        val Keep: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keep)

        val KeepOf: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_keep_off)

        val ContentCopy: Painter
            @Composable
            get() = painterResource(Res.drawable.ic_content_copy)

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
