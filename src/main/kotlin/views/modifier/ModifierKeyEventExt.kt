package views.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*


fun Modifier.onEnterKey(block: () -> Unit) = this.onKeyEvent { event ->
    if (event.type != KeyEventType.KeyUp) {
        return@onKeyEvent false
    }
    if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
        block()
        true
    } else {
        false
    }
}
