package com.virogu.pager.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.virogu.bean.AdbDevice
import com.virogu.tools.Tools
import theme.dropdownMenuItemPadding
import theme.textFieldContentPadding
import views.OutlinedTextField

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:28:25
 **/

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectDeviceView(
    modifier: Modifier = Modifier, currentDevice: AdbDevice?, tools: Tools
) {
    val connectTool = tools.deviceConnectTool
    val devices = connectTool.connectedDevice.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxSize(),
            readOnly = true,
            singleLine = true,
            value = currentDevice?.showName.orEmpty(),
            onValueChange = {},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            contentPadding = textFieldContentPadding()
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