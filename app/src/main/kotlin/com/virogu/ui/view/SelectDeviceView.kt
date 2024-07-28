package com.virogu.ui.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.virogu.core.device.Device
import com.virogu.core.tool.Tools
import theme.dropdownMenuItemPadding
import views.OutlinedText

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:28:25
 **/

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectDeviceView(
    modifier: Modifier = Modifier, currentDevice: Device?, tools: Tools
) {
    val connectTool = tools.deviceConnect
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