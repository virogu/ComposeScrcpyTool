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

@file:Suppress("FunctionName")

package com.virogu.ui.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virogu.core.tool.init.InitState

@Composable
fun InitPager(initState: State<InitState>) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = initState.value.msg,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            val subMsg = initState.value.subMsg.trim()
            if (subMsg.isEmpty()) {
                return
            }
            SelectionContainer {
                Text(
                    text = subMsg,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().background(
                        color = MaterialTheme.colors.onBackground.copy(0.2f),
                        shape = TextFieldDefaults.OutlinedTextFieldShape
                    ).padding(8.dp)
                )
            }
        }
    }
}