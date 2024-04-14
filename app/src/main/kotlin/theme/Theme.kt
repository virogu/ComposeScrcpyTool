@file:Suppress("FunctionName")

package theme

import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200,
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun MainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = if (darkTheme) {
        DarkColorPalette
        //LightColorPalette
    } else {
        LightColorPalette
    }
    val contextMenuRepresentation = if (darkTheme) {
        DarkDefaultContextMenuRepresentation
    } else {
        LightDefaultContextMenuRepresentation
    }
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
    ) {
        CompositionLocalProvider(LocalContextMenuRepresentation provides contextMenuRepresentation) {
            Surface(
                modifier = modifier,
            ) {
                Box(Modifier.fillMaxSize(), content = content)
            }
        }
    }
}