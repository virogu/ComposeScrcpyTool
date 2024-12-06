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

package com.virogu.ui.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:29:45
 **/

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OptionButton(
    description: String,
    enable: Boolean,
    painter: Painter,
    elevation: ButtonElevation? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(6.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
    onClick: () -> Unit
) {
    val click by rememberUpdatedState(onClick)
    val modifier = Modifier.fillMaxHeight().aspectRatio(1f)
    val iconModifier = Modifier
    // wrap button in BoxWithTooltip
    TooltipArea(
        tooltip = {
            Card(elevation = 4.dp) {
                Text(text = description, modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 500, // in milliseconds
    ) {
        Button(
            onClick = {
                click()
            },
            enabled = enable,
            modifier = modifier,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
            elevation = elevation,
            border = border
        ) {
            Icon(modifier = iconModifier, painter = painter, contentDescription = description)
        }
    }
}