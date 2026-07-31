package theme.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import theme.Icon

@Suppress("UnusedReceiverParameter")
val Icon.Outlined.NorthEast: Painter
    @Composable
    get() = rememberVectorPainter(north_east)

val north_east: ImageVector by lazy {
    ImageVector.Builder(
        name = "north_east",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(5.4f, 20f)
            lineTo(4f, 18.6f)
            lineTo(15.6f, 7f)
            horizontalLineTo(9f)
            verticalLineTo(5f)
            horizontalLineTo(19f)
            verticalLineTo(15f)
            horizontalLineTo(17f)
            verticalLineTo(8.4f)
            lineTo(5.4f, 20f)
            close()
        }
    }.build()
}