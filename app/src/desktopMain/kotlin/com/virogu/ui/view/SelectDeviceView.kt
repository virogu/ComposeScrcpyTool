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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.virogu.core.device.Device
import com.virogu.core.tool.connect.DeviceConnect
import theme.dropdownMenuItemPadding
import views.OutlinedText

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:28:25
 **/

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectDeviceView(
    modifier: Modifier = Modifier,
    currentDevice: Device?,
    connectTool: DeviceConnect
) {
    val devices = connectTool.connectedDevice.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedText(
            modifier = Modifier.fillMaxSize(),
            value = currentDevice?.showName.orEmpty(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            devices.value.forEach {
                DropdownMenuItem(
                    onClick = {
                        connectTool.selectDevice(it)
                        expanded = false
                    },
                    contentPadding = dropdownMenuItemPadding(),
                ) {
                    Text(text = it.showName)
                }
            }
        }
    }
}