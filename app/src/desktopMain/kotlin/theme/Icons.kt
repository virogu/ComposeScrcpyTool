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
import androidx.compose.ui.res.painterResource
import composescrcpytool.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource

object Icon {
    object Outlined
    object Filled
}

@Composable
fun ImageVector.painter() = rememberVectorPainter(this)

@Composable
private fun fromResource(resourcePath: String) = painterResource(resourcePath)

val Icon.Logo: Painter
    @Composable
    get() = painterResource(Res.drawable.logo)

//Outlined
val Icon.Outlined.AdminPanelSettings: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_admin_panel_settings)

val Icon.Outlined.ClockLoader: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_clock_loader)

val Icon.Outlined.Construction: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_construction)

val Icon.Outlined.Dangerous: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_dangerous)

val Icon.Outlined.TrashCan: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_delete)

val Icon.Outlined.DesktopAndPhone: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_device_connection)

val Icon.Outlined.Download: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_download)

val Icon.Outlined.FileDocument: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_file)

val Icon.Outlined.FileLink: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_file_link)

val Icon.Outlined.FileFolder: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_folder)

val Icon.Outlined.FileFolderOpen: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_folder_open)

val Icon.Outlined.FileNewFile: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_new_file)

val Icon.Outlined.FileNewFolder: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_new_folder)

val Icon.Outlined.FileUnknown: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_unknown_file)

val Icon.Outlined.CPU: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_memory)

val Icon.Outlined.SmartPhone: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_smartphone)

val Icon.Outlined.Star: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_star)

val Icon.Outlined.Stop: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_stop)

val Icon.Outlined.Sync: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_sync)

val Icon.Outlined.Upload: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_upload)

val Icon.Outlined.PowerSetting: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_power_settings_new)

val Icon.Outlined.VolumeDown: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_volume_down_alt)

val Icon.Outlined.VolumeUp: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_volume_up)

val Icon.Outlined.Preview: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_preview)

val Icon.Outlined.PreviewOff: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_preview_off)

val Icon.Outlined.FilterNone: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_filter_none)

val Icon.Outlined.SelectWindow: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_select_window)

val Icon.Outlined.KeyboardDoubleArrowDown: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_keyboard_double_arrow_down)

val Icon.Outlined.Screenshot: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_screenshot)

val Icon.Outlined.DragPan: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_drag_pan)

val Icon.Outlined.MoreHorizon: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_more_horiz)

val Icon.Outlined.Keep: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_keep)

val Icon.Outlined.KeepOf: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_keep_off)

val Icon.Outlined.ContentCopy: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_content_copy)

//Filled
val Icon.Filled.Star: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_star_fill)
val Icon.Filled.FileFolder: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_folder_fill)
val Icon.Filled.FileFolderOpen: Painter
    @Composable
    get() = painterResource(Res.drawable.ic_folder_open_fill)