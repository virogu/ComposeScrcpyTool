package views

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


fun Modifier.defaultTextSize() = this.defaultMinSize(
    minWidth = 80.dp,
    minHeight = 56.dp
)
