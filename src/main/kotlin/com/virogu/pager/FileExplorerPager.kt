@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virogu.bean.FileInfo
import com.virogu.bean.FileType
import com.virogu.tools.Tools
import com.virogu.tools.explorer.FileExplorerImpl
import theme.materialColors

@Composable
fun FileExplorerPager(
    tools: Tools,
    fileListState: LazyListState,
) {
    Column {
        Row(Modifier.fillMaxSize().padding(8.dp)) {
            val fileExplorer = tools.fileExplorer
            val scrollAdapter = rememberScrollbarAdapter(fileListState)
            val currentSelect = remember(tools.deviceConnectTool.currentSelectedDevice.value) {
                mutableStateOf(FileInfo.ROOT)
            }
            LazyColumn(
                Modifier.fillMaxHeight().weight(1f).padding(8.dp),
                state = fileListState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                fileExplorer.getChild(FileInfo.ROOT).forEach {
                    FileView(
                        fileInfo = it,
                        fileExplorer = fileExplorer,
                        currentSelect = currentSelect,
                        expanded = fileExplorer.expandedMap,
                        level = 0
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

private val ERROR_COLOR = Color(0xFFDA4C3F)
private val TIPS_COLOR = Color(0XFF5B9027)

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.FileView(
    fileInfo: FileInfo,
    fileExplorer: FileExplorerImpl,
    currentSelect: MutableState<FileInfo>,
    expanded: SnapshotStateMap<String, Boolean>,
    level: Int,
) {
    val currentExpanded = expanded[fileInfo.path] ?: false
    item {
        val selected = remember(fileInfo.path, currentSelect.value) {
            mutableStateOf(currentSelect.value == fileInfo)
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
                            currentSelect.value = fileInfo
                            expanded[fileInfo.path] = !currentExpanded
                        }, onClick = {
                            currentSelect.value = fileInfo
                        })
                    }

                    FileType.FILE -> {
                        clickable {
                            currentSelect.value = fileInfo
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
                                currentSelect.value = fileInfo
                                expanded[fileInfo.path] = !currentExpanded
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
        fileExplorer.getChild(fileInfo).forEach {
            FileView(it, fileExplorer, currentSelect, expanded, level + 1)
        }
    }
}