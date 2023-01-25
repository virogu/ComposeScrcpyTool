@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.virogu.bean.FileInfo
import com.virogu.bean.FileType
import com.virogu.tools.Tools
import theme.Purple200
import theme.materialColors

@Composable
fun FileExplorerPager(
    tools: Tools,
    fileListState: LazyListState,
) {
    Box {
        val fileExplorer = tools.fileExplorer
        val scrollAdapter = rememberScrollbarAdapter(fileListState)
        var currentSelect: FileInfo? by remember(tools.deviceConnectTool.currentSelectedDevice.value) {
            mutableStateOf(null)
        }
        val selectFile by rememberUpdatedState { file: FileInfo ->
            currentSelect = file
        }
        val getExpended by rememberUpdatedState { file: FileInfo ->
            fileExplorer.expandedMap[file.path] ?: false
        }
        val setExpended by rememberUpdatedState { file: FileInfo, expand: Boolean ->
            fileExplorer.expandedMap[file.path] = expand
        }
        val getChildFiles by rememberUpdatedState { file: FileInfo ->
            fileExplorer.getChild(file)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectDeviceView(tools, Modifier.padding(horizontal = 16.dp).height(40.dp))
            ToolBarView(tools, currentSelect)
            Row {
                LazyColumn(
                    Modifier.fillMaxHeight().weight(1f),
                    state = fileListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    getChildFiles(FileInfo.ROOT).forEach {
                        FileView(
                            fileInfo = it,
                            currentSelect = currentSelect,
                            level = 0,
                            getChildFiles = getChildFiles,
                            selectFile = selectFile,
                            getExpended = getExpended,
                            setExpended = setExpended,
                        )
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier,
                    adapter = scrollAdapter,
                    reverseLayout = false,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SelectDeviceView(
    tools: Tools,
    modifier: Modifier = Modifier
) {
    val connectTool = tools.deviceConnectTool

    val current = connectTool.currentSelectedDevice.collectAsState()
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
            borderStroke,
            TextFieldDefaults.OutlinedTextFieldShape
        ).clickable {
            expanded.value = true
        }.onPlaced {
            dropMenuWidth.value = it.size.width.dp
        }.align(Alignment.CenterHorizontally),
    ) {
        Row {
            Text(
                text = current.value?.showName.orEmpty(),
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterVertically).weight(1f).padding(horizontal = 16.dp)
            )
            Button(
                onClick = {
                    expanded.value = !expanded.value
                },
                modifier = Modifier.fillMaxHeight().aspectRatio(1f).align(Alignment.CenterVertically),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 0, 0, alpha = 0)),
                contentPadding = PaddingValues(4.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(Icons.Default.ArrowDropDown, "", tint = contentColorFor(MaterialTheme.colors.background))
            }
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
            },
            modifier = Modifier.width(dropMenuWidth.value),
            offset = DpOffset(dropMenuOffset.value, 0.dp)
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

@Composable
private fun ToolBarView(tools: Tools, currentSelect: FileInfo?) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(35.dp)) {
        Row(Modifier.align(Alignment.CenterStart), Arrangement.spacedBy(8.dp)) {
            val modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            val shape = RoundedCornerShape(8.dp)
            val colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
            val contentPadding = PaddingValues(6.dp)
            val iconModifier = Modifier
            Button(
                onClick = {
                },
                enabled = currentSelect != null && currentSelect.type == FileType.DIR,
                modifier = modifier, shape = shape, colors = colors, contentPadding = contentPadding
            ) {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource("icons/icon_new_folder.svg"),
                    contentDescription = "新建文件夹",
                )
            }
            Button(
                onClick = {
                },
                enabled = currentSelect != null && currentSelect.type == FileType.DIR,
                modifier = modifier, shape = shape, colors = colors, contentPadding = contentPadding
            ) {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource("icons/ic_new_file.svg"),
                    contentDescription = "新建文件",
                )
            }
        }
    }
}

private val ERROR_COLOR = Color(0xFFDA4C3F)
private val TIPS_COLOR = Purple200.copy(alpha = 0.8f)

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.FileView(
    fileInfo: FileInfo,
    currentSelect: FileInfo?,
    level: Int,
    getChildFiles: (FileInfo) -> List<FileInfo>,
    selectFile: (FileInfo) -> Unit,
    getExpended: (FileInfo) -> Boolean,
    setExpended: (FileInfo, Boolean) -> Unit
) {
    val currentExpanded = getExpended(fileInfo)
    item {
        val selected = remember(fileInfo.path, currentSelect) {
            mutableStateOf(currentSelect == fileInfo)
        }
        val selectedColor = materialColors.primary.copy(alpha = 0.5f)
        val backgroundColor = remember(selected.value) {
            val v = if (selected.value) {
                selectedColor
            } else {
                Color.Transparent
            }
            mutableStateOf(v)
        }
        Card(
            modifier = Modifier.height(40.dp).clickable { }.run {
                when (fileInfo.type) {
                    FileType.DIR -> {
                        onClick(true, onDoubleClick = {
                            selectFile(fileInfo)
                            setExpended(fileInfo, !currentExpanded)
                        }, onClick = {
                            selectFile(fileInfo)
                        })
                    }

                    FileType.FILE -> {
                        clickable {
                            selectFile(fileInfo)
                        }
                    }

                    else -> {
                        this
                    }
                }
            },
            backgroundColor = backgroundColor.value,
            elevation = 0.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modifier = Modifier.align(Alignment.CenterVertically)
                val iconModifier = modifier.size(20.dp)
                Spacer(Modifier.width((level * 8).dp))
                when (fileInfo.type) {
                    FileType.DIR -> {
                        Icon(
                            modifier = modifier.size(30.dp).clickable {
                                selectFile(fileInfo)
                                setExpended(fileInfo, !currentExpanded)
                            }.padding(5.dp),
                            imageVector = if (currentExpanded) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.Filled.KeyboardArrowRight
                            },
                            contentDescription = fileInfo.name
                        )
                        Icon(
                            modifier = iconModifier,
                            painter = painterResource("icons/ic_folder.svg"),
                            contentDescription = fileInfo.name
                        )
                    }

                    FileType.LINK -> {
                        Spacer(iconModifier)
                        Icon(
                            modifier = iconModifier,
                            painter = painterResource("icons/ic_file_link.svg"),
                            contentDescription = fileInfo.name
                        )
                    }

                    FileType.FILE -> {
                        Spacer(iconModifier)
                        Icon(
                            modifier = iconModifier,
                            painter = painterResource("icons/ic_file.svg"),
                            contentDescription = fileInfo.name
                        )
                    }

                    FileType.OTHER -> {
                        Spacer(iconModifier)
                        Icon(
                            modifier = iconModifier,
                            painter = painterResource("icons/ic_unknown_file.svg"),
                            contentDescription = fileInfo.name
                        )
                    }

                    else -> {
                        Spacer(iconModifier)
                        Spacer(iconModifier)
                    }
                }
                Text(
                    text = fileInfo.name,
                    color = when (fileInfo.type) {
                        FileType.ERROR -> ERROR_COLOR
                        FileType.TIPS -> TIPS_COLOR
                        else -> Color.Unspecified
                    },
                    modifier = modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
    if (currentExpanded) {
        getChildFiles(fileInfo).forEach {
            FileView(
                fileInfo = it,
                currentSelect = currentSelect,
                level = level + 1,
                getChildFiles = getChildFiles,
                selectFile = selectFile,
                getExpended = getExpended,
                setExpended = setExpended,
            )
        }
    }
}