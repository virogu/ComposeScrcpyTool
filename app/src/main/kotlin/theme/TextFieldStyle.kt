package theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.textFieldWithLabelHeight() = this.height(56.dp)

fun Modifier.textFieldHeight() = this.height(48.dp)

fun textFieldContentPadding() = PaddingValues(16.dp, 0.dp)

fun dropdownMenuItemPadding() = PaddingValues(16.dp, 0.dp)
