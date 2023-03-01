package com.virogu.pager.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.virogu.bean.AdbDevice
import com.virogu.tools.Tools

/**
 * Created by Virogu
 * Date 2023/08/03 下午 3:28:25
 **/

@Composable
fun SelectDeviceView(
    modifier: Modifier = Modifier, currentDevice: AdbDevice?, tools: Tools
) {
    val connectTool = tools.deviceConnectTool

    val devices = connectTool.connectedDevice.collectAsState()

    val expanded = remember { mutableStateOf(false) }
    val borderStroke by com.virogu.pager.view.animateBorderStrokeAsState()
    val dropMenuWidth = remember {
        mutableStateOf(0.dp)
    }
    val dropMenuOffset = remember {
        mutableStateOf(0.dp)
    }
    Box(
        modifier = modifier.border(
            borderStroke, TextFieldDefaults.OutlinedTextFieldShape
        ).clickable {
            expanded.value = true
        }.onPlaced {
            dropMenuWidth.value = it.size.width.dp
        },
    ) {
        Row {
            Text(
                text = currentDevice?.showName.orEmpty(),
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterVertically).weight(1f).padding(horizontal = 16.dp)
            )
            Button(
                onClick = {
                    expanded.value = !expanded.value
                },
                modifier = Modifier.fillMaxHeight().aspectRatio(1f).align(Alignment.CenterVertically),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                contentPadding = PaddingValues(4.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Default.ArrowDropDown, "", tint = contentColorFor(MaterialTheme.colors.background))
            }
        }
        DropdownMenu(
            expanded = expanded.value, onDismissRequest = {
                expanded.value = false
            }, modifier = Modifier.width(dropMenuWidth.value), offset = DpOffset(dropMenuOffset.value, 0.dp)
        ) {
            devices.value.forEach {
                Column(modifier = Modifier.clickable {
                    connectTool.selectDevice(it)
                    expanded.value = false
                }) {
                    Text(text = it.showName, modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 10.dp))
                    //Box(modifier = Modifier.fillMaxWidth().padding(16.dp).height(0.5.dp).background(Color.LightGray))
                }
            }
        }
    }
}