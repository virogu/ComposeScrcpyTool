/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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