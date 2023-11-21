package theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

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
    get() = fromResource("logo.png")

//Outlined
val Icon.Outlined.AdminPanelSettings: Painter
    @Composable
    get() = fromResource("icons/ic_admin_panel_settings.svg")

val Icon.Outlined.ClockLoader: Painter
    @Composable
    get() = fromResource("icons/ic_clock_loader.svg")

val Icon.Outlined.Construction: Painter
    @Composable
    get() = fromResource("icons/ic_construction.svg")

val Icon.Outlined.Dangerous: Painter
    @Composable
    get() = fromResource("icons/ic_dangerous.svg")

val Icon.Outlined.TrashCan: Painter
    @Composable
    get() = fromResource("icons/ic_delete.svg")

val Icon.Outlined.DesktopAndPhone: Painter
    @Composable
    get() = fromResource("icons/ic_device_connection.svg")

val Icon.Outlined.Download: Painter
    @Composable
    get() = fromResource("icons/ic_download.svg")

val Icon.Outlined.FileDocument: Painter
    @Composable
    get() = fromResource("icons/ic_file.svg")

val Icon.Outlined.FileLink: Painter
    @Composable
    get() = fromResource("icons/ic_file_link.svg")

val Icon.Outlined.FileFolder: Painter
    @Composable
    get() = fromResource("icons/ic_folder.svg")

val Icon.Outlined.FileFolderOpen: Painter
    @Composable
    get() = fromResource("icons/ic_folder_open.svg")

val Icon.Outlined.FileNewFile: Painter
    @Composable
    get() = fromResource("icons/ic_new_file.svg")

val Icon.Outlined.FileNewFolder: Painter
    @Composable
    get() = fromResource("icons/ic_new_folder.svg")

val Icon.Outlined.FileUnknown: Painter
    @Composable
    get() = fromResource("icons/ic_unknown_file.svg")

val Icon.Outlined.CPU: Painter
    @Composable
    get() = fromResource("icons/ic_memory.svg")

val Icon.Outlined.SmartPhone: Painter
    @Composable
    get() = fromResource("icons/ic_smartphone.svg")

val Icon.Outlined.Star: Painter
    @Composable
    get() = fromResource("icons/ic_star.svg")

val Icon.Outlined.Stop: Painter
    @Composable
    get() = fromResource("icons/ic_stop.svg")

val Icon.Outlined.Sync: Painter
    @Composable
    get() = fromResource("icons/ic_sync.svg")

val Icon.Outlined.Upload: Painter
    @Composable
    get() = fromResource("icons/ic_upload.svg")

val Icon.Outlined.PowerSetting: Painter
    @Composable
    get() = fromResource("icons/ic_power_settings_new.svg")

val Icon.Outlined.VolumeDown: Painter
    @Composable
    get() = fromResource("icons/ic_volume_down_alt.svg")

val Icon.Outlined.VolumeUp: Painter
    @Composable
    get() = fromResource("icons/ic_volume_up.svg")

val Icon.Outlined.Preview: Painter
    @Composable
    get() = fromResource("icons/ic_preview.svg")

val Icon.Outlined.PreviewOff: Painter
    @Composable
    get() = fromResource("icons/ic_preview_off.svg")

val Icon.Outlined.FilterNone: Painter
    @Composable
    get() = fromResource("icons/ic_filter_none.svg")

val Icon.Outlined.SelectWindow: Painter
    @Composable
    get() = fromResource("icons/ic_select_window.svg")

val Icon.Outlined.KeyboardDoubleArrowDown: Painter
    @Composable
    get() = fromResource("icons/ic_keyboard_double_arrow_down.svg")

val Icon.Outlined.DragPan: Painter
    @Composable
    get() = fromResource("icons/ic_drag_pan.svg")

val Icon.Outlined.MoreHorizon: Painter
    @Composable
    get() = fromResource("icons/ic_more_horiz.svg")

val Icon.Outlined.ContentCopy: Painter
    @Composable
    get() = fromResource("icons/ic_content_copy.svg")

//Filled
val Icon.Filled.Star: Painter
    @Composable
    get() = fromResource("icons/ic_star_fill.svg")
val Icon.Filled.FileFolder: Painter
    @Composable
    get() = fromResource("icons/ic_folder_fill.svg")
val Icon.Filled.FileFolderOpen: Painter
    @Composable
    get() = fromResource("icons/ic_folder_open_fill.svg")