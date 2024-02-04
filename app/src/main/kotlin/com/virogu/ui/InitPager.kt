@file:Suppress("FunctionName")

package com.virogu.ui

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
import com.virogu.core.tool.init.InitTool

@Composable
fun InitPager(initState: State<InitTool.State>) {
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
            SelectionContainer {
                Text(
                    text = initState.value.subMsg.trim(),
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