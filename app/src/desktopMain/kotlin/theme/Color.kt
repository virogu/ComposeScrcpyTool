package theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

val Purple200 = Color(0xFFA871FF)
val Purple500 = Color(0xFFA871FF)
val Purple700 = Color(0xFFA871FF)
val Teal200 = Color(0xFF03DAC5)

val Red_500 = Color(0xFFF44336)

val materialColors
    @Composable
    get() = MaterialTheme.colors

@Composable
fun rememberItemBackground(
    selected: Boolean,
    focused: Boolean? = null,
): MutableState<Color> {
    val primaryColor = materialColors.primary.copy(alpha = 0.5f)
    return remember(selected, focused) {
        val c = if (selected) {
            primaryColor.copy(alpha = 0.5f)
        } else if (focused == true) {
            primaryColor.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        }
        mutableStateOf(c)
    }
}