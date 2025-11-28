/*
 * Copyright 2025 Virogu
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

package com.virogu.ui.pager

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.clipEntryOf
import androidx.compose.ui.platform.setText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virogu.core.bean.FileType
import com.virogu.core.bean.RemoteFile
import com.virogu.core.bean.RemoteFileLoadState
import com.virogu.core.device.Device
import com.virogu.core.tool.Tools
import com.virogu.core.viewmodel.RemoteFileViewModel
import com.virogu.ui.dialog.*
import com.virogu.ui.view.BusyProcessView
import com.virogu.ui.view.OptionButton
import com.virogu.ui.view.SelectDeviceView
import com.virogu.ui.view.TipsView
import kotlinx.coroutines.launch
import theme.Icon
import theme.materialColors
import theme.textFieldHeight
import java.net.URI
import kotlin.io.path.toPath

enum class FileOptionDialogType {
    Close,
    NewFolder,
    NewFile,
    Download,
    Upload,
    Delete,
    Chmod,
}

@Composable
fun RemoteFilePager(
    tools: Tools,
    fileListState: LazyListState,
    model: RemoteFileViewModel = viewModel(),
) {
    val scrollAdapter = rememberScrollbarAdapter(fileListState)
    val selectedDevice by tools.deviceConnect.currentSelectedDevice.collectAsState()
    val selectedOnlineDevice: Device? by remember(selectedDevice) {
        mutableStateOf(selectedDevice?.takeIf { it.isOnline })
    }
    var selectedFile: RemoteFile? by remember(selectedDevice) {
        mutableStateOf(null)
    }
    val (optionDialogType, updateDialogType) = remember(selectedDevice) {
        mutableStateOf(FileOptionDialogType.Close)
    }
    val rootFile by remember { mutableStateOf(RemoteFile.ROOT) }

    val parentChildren = selectedFile?.parent?.children?.value
    val parentChildrenLoadStates = selectedFile?.parent?.childrenLoadStates?.value
    LaunchedEffect(rootFile.children.value, rootFile.childrenLoadStates) {
        if (rootFile.childrenLoadStates.value is RemoteFileLoadState.Loaded) {
            selectedFile = null
        }
    }
    LaunchedEffect(parentChildren, parentChildrenLoadStates) {
        val current = selectedFile ?: return@LaunchedEffect
        val parent = current.parent ?: return@LaunchedEffect
        if (parent.childrenLoadStates.value is RemoteFileLoadState.Loaded) {
            val children = parent.children.value
            val newFile = children.find { it.path == current.path }
            if (newFile == null) {
                // 文件不在当前列表中了（被删除了），清空选中状态
                selectedFile = null
            } else if (newFile !== current) {
                // 文件还在，但对象引用变了（刷新了），更新为新对象
                selectedFile = newFile
            }
        }
    }

    Box {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectDeviceView(
                Modifier.textFieldHeight().align(Alignment.CenterHorizontally).padding(horizontal = 16.dp),
                selectedDevice,
                tools.deviceConnect
            )
            ToolBarView(
                model = model,
                selectedFile = selectedFile,
                device = selectedDevice,
                onRefreshRoot = {
                    model.refresh(rootFile)
                },
                onRemountRoot = {
                    selectedOnlineDevice?.also {
                        model.restartWithRoot(it, onFinished = {
                            rootFile.refresh()
                        })
                    }
                },
                showOptionDialog = updateDialogType
            )
            if (selectedOnlineDevice != null) {
                Row {
                    LazyColumn(
                        Modifier.fillMaxHeight().weight(1f).onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyUp) {
                                return@onKeyEvent false
                            }
                            when (event.key) {
                                Key.F5 -> {
                                    model.refresh(selectedFile ?: rootFile)
                                    true
                                }

                                Key.Delete -> {
                                    updateDialogType(FileOptionDialogType.Delete)
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        },
                        state = fileListState,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FileTreeRecursive(
                            model,
                            selectedOnlineDevice,
                            rootFile,
                            selectedFile,
                            onSelectFile = { file ->
                                selectedFile = file
                            },
                            showOptionDialog = updateDialogType
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    VerticalScrollbar(
                        modifier = Modifier,
                        adapter = scrollAdapter,
                        reverseLayout = false,
                    )
                }
            } else {
                FilePlaceholdersItem(
                    "No Online Device",
                    materialColors.error,
                    0
                )
            }
        }
        TipsView(Modifier.align(Alignment.BottomCenter), model.tipsFlow)
    }
    FileOptionDialog(
        model, optionDialogType, selectedOnlineDevice, selectedFile,
        onClose = { updateDialogType(FileOptionDialogType.Close) }
    )
}

@Composable
fun FileOptionDialog(
    model: RemoteFileViewModel,
    type: FileOptionDialogType,
    device: Device?,
    file: RemoteFile?,
    onClose: () -> Unit
) {
    if (device == null) {
        onClose()
        return
    }
    if (file == null) {
        onClose()
        return
    }
    when (type) {
        FileOptionDialogType.Close -> {}
        FileOptionDialogType.NewFolder -> RemoteNewFolderDialog(model, device, file, onClose)
        FileOptionDialogType.NewFile -> RemoteNewFileDialog(model, device, file, onClose)
        FileOptionDialogType.Download -> RemoteDownloadDialog(model, device, file, onClose)
        FileOptionDialogType.Upload -> RemoteUploadDialog(model, device, file, onClose)
        FileOptionDialogType.Delete -> RemoteDeleteDialog(model, device, file, onClose)
        FileOptionDialogType.Chmod -> RemoteChmodDialog(model, device, file, onClose)
    }
}

@Suppress("FunctionName")
fun LazyListScope.FileTreeRecursive(
    model: RemoteFileViewModel,
    device: Device?,
    file: RemoteFile,
    selectedFile: RemoteFile?,
    onSelectFile: (RemoteFile?) -> Unit,
    showOptionDialog: (FileOptionDialogType) -> Unit,
) {
    if (device == null) {
        return
    }
    //ohos系统里面文件夹下居然可以存在两个完全相同的文件，这里用文件路径作为key会重复
    //item(key = fileItem.path) {
    item(key = null) {
        LaunchedEffect(file.isExpanded.value, file.childrenLoadStates.value) {
            if (file.isDirectory &&
                file.isExpanded.value &&
                file.childrenLoadStates.value == RemoteFileLoadState.NotLoad
            ) {
                model.loadChildren(device, file)
            }
        }
        //不显示根目录
        if (file.path.isNotEmpty()) {
            FileNodeItem(model, device, file, selectedFile, onSelectFile, showOptionDialog)
        }
    }

    if (file.isExpanded.value) {
        when (val state = file.childrenLoadStates.value) {
            RemoteFileLoadState.Loaded -> {
                if (file.children.value.isEmpty()) {
                    item {
                        FilePlaceholdersItem(
                            "(Empty)",
                            materialColors.primary.copy(alpha = 0.8f),
                            file.level
                        )
                    }
                } else {
                    file.children.value.forEach { child ->
                        FileTreeRecursive(model, device, child, selectedFile, onSelectFile, showOptionDialog)
                    }
                }
            }

            RemoteFileLoadState.Loading -> item {
                FilePlaceholdersItem(
                    "Loading...",
                    materialColors.primary.copy(alpha = 0.8f),
                    file.level
                )
            }

            is RemoteFileLoadState.Error -> item {
                FilePlaceholdersItem(
                    state.msg,
                    materialColors.error,
                    file.level
                )
            }

            RemoteFileLoadState.NotLoad -> item {
                FilePlaceholdersItem(
                    "Waiting...",
                    materialColors.primary.copy(alpha = 0.8f),
                    file.level
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileNodeItem(
    model: RemoteFileViewModel,
    device: Device,
    file: RemoteFile,
    currentSelect: RemoteFile?,
    selectFile: (RemoteFile?) -> Unit,
    showOptionDialog: (FileOptionDialogType) -> Unit,
    primaryColor: Color = materialColors.primary.copy(alpha = 0.5f)
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var mouseEnter by remember { mutableStateOf(false) }
    val backgroundColor = remember(currentSelect?.path, file.path, mouseEnter) {
        when {
            currentSelect?.path == file.path -> primaryColor.copy(alpha = 0.5f)
            mouseEnter -> primaryColor.copy(alpha = 0.2f)
            else -> Color.Transparent
        }
    }

    val contextMenuState = remember { ContextMenuState() }
    val dragAndDropTargetCallback = remember(file) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                mouseEnter = true
                super.onEntered(event)
            }

            override fun onExited(event: DragAndDropEvent) {
                mouseEnter = false
                super.onExited(event)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                println("event ${event.action}")
                event.dragData().also { dragData ->
                    if (dragData !is DragData.FilesList) {
                        return@also
                    }
                    val files = dragData.readFiles().mapNotNull { uri ->
                        try {
                            URI.create(uri).toPath().toFile()
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    model.pushFile(device, file, *files.toTypedArray())
                    println("onDrop $files")
                }
                return true
            }
        }
    }

    LaunchedEffect(contextMenuState.status) {
        if (contextMenuState.status is ContextMenuState.Status.Open) {
            selectFile(file)
        }
    }

    val refreshPath by rememberUpdatedState {
        model.refresh(file)
    }

    val copyName by rememberUpdatedState {
        scope.launch {
            clipboard.setText(file.name)
        }
        model.emitTips("${file.name} 名称已复制")
    }
    val copyPath by rememberUpdatedState {
        scope.launch {
            clipboard.setText(file.path)
        }
        model.emitTips("${file.path} 路径已复制")
    }
    val getFileDetails by rememberUpdatedState {
        model.getFileDetails(device, file)
    }

    ContextMenuArea(state = contextMenuState, items = {
        mutableListOf<ContextMenuItem>().apply {
            ContextMenuItem("刷新", refreshPath).also(::add)
            if (file.isDirectory) {
                ContextMenuItem("新建文件夹") { showOptionDialog(FileOptionDialogType.NewFolder) }.also(::add)
                ContextMenuItem("新建文件") { showOptionDialog(FileOptionDialogType.NewFile) }.also(::add)
                ContextMenuItem("导入文件") { showOptionDialog(FileOptionDialogType.Upload) }.also(::add)
            }
            ContextMenuItem("导出文件") { showOptionDialog(FileOptionDialogType.Download) }.also(::add)
            ContextMenuItem("复制名称", copyName).also(::add)
            ContextMenuItem("复制路径", copyPath).also(::add)
            ContextMenuItem("显示详细信息", getFileDetails).also(::add)
            ContextMenuItem("修改权限") { showOptionDialog(FileOptionDialogType.Chmod) }.also(::add)
            ContextMenuItem("删除") { showOptionDialog(FileOptionDialogType.Delete) }.also(::add)
        }
    }) {
        Card(
            backgroundColor = backgroundColor, elevation = 0.dp,
            modifier = Modifier.height(40.dp).onPointerEvent(PointerEventType.Enter) {
                mouseEnter = true
            }.onPointerEvent(PointerEventType.Exit) {
                mouseEnter = false
            }.onPointerEvent(PointerEventType.Release) {
                mouseEnter = false
            }.combinedClickable(onClick = {}, onDoubleClick = {
                file.toggleExpand()
            }).run {
                when (file.type) {
                    FileType.DIR -> {
                        dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dragAndDropTargetCallback)
                    }

                    else -> this
                }
            }.onPointerEvent(PointerEventType.Press) { selectFile(file) }
        ) {
            val nameHasVisualOverflow = remember { mutableStateOf(false) }
            TooltipArea(
                tooltip = {
                    if (nameHasVisualOverflow.value && contextMenuState.status !is ContextMenuState.Status.Open) {
                        Card(elevation = 4.dp) {
                            Text(text = file.name, modifier = Modifier.padding(8.dp))
                        }
                    }
                },
                delayMillis = 500,
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.dp).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modifier = Modifier.align(Alignment.CenterVertically)
                    val iconModifier = modifier.size(20.dp)
                    Row(
                        modifier = modifier.weight(5f), horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(Modifier.width((file.level * 8).dp))
                        when (file.type) {
                            FileType.DIR -> {
                                Icon(
                                    modifier = modifier.size(30.dp).clickable(role = Role.Button) {
                                        selectFile(file)
                                        file.toggleExpand()
                                    }.padding(5.dp), painter = if (file.isExpanded.value) {
                                        Icon.Outlined.KeyboardArrowDown
                                    } else {
                                        Icon.Outlined.KeyboardArrowRight
                                    }, contentDescription = file.name
                                )
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileFolder,
                                    contentDescription = file.name
                                )
                            }

                            FileType.LINK -> {
                                Spacer(iconModifier)
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileLink,
                                    contentDescription = file.name
                                )
                            }

                            FileType.FILE -> {
                                Spacer(iconModifier)
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileDocument,
                                    contentDescription = file.name
                                )
                            }

                            FileType.OTHER -> {
                                Spacer(iconModifier)
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileUnknown,
                                    contentDescription = file.name
                                )
                            }

                        }
                        Text(
                            text = file.name,
                            color = materialColors.onSurface,
                            modifier = modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = {
                                nameHasVisualOverflow.value = it.hasVisualOverflow
                            },
                            maxLines = 1
                        )
                    }
                    Text(
                        text = file.permissions,
                        color = materialColors.onSurface.copy(0.8f),
                        modifier = modifier.weight(2f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = file.modificationTime,
                        color = materialColors.onSurface.copy(0.8f),
                        modifier = modifier.weight(2f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = file.size,
                        color = materialColors.onSurface.copy(0.8f),
                        overflow = TextOverflow.Ellipsis,
                        modifier = modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1,
                    )
                }
            }
        }
    }

}

@Composable
fun FilePlaceholdersItem(
    text: String,
    color: Color,
    level: Int,
    modifier: Modifier = Modifier.height(40.dp).padding(end = 8.dp)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val modifier = Modifier.align(Alignment.CenterVertically)
        Spacer(Modifier.width((level * 8).dp.plus(40.dp)))
        Text(
            text = text, color = color,
            modifier = modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
private fun ToolBarView(
    model: RemoteFileViewModel,
    selectedFile: RemoteFile?,
    device: Device?,
    onRemountRoot: () -> Unit,
    onRefreshRoot: () -> Unit,
    showOptionDialog: (FileOptionDialogType) -> Unit,
) {
    val deviceConnected = device?.isOnline == true
    val isBusy by model.isBusy.collectAsState()
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(35.dp)) {
        Row(Modifier.align(Alignment.CenterStart), Arrangement.spacedBy(8.dp)) {
            OptionButton(
                "新建文件夹",
                enable = deviceConnected && selectedFile?.type == FileType.DIR && !isBusy,
                painter = Icon.Outlined.FileNewFolder
            ) {
                showOptionDialog(FileOptionDialogType.NewFolder)
            }
            OptionButton(
                "新建文件",
                enable = deviceConnected && selectedFile?.type == FileType.DIR && !isBusy,
                painter = Icon.Outlined.FileNewFile
            ) {
                showOptionDialog(FileOptionDialogType.NewFile)
            }
            OptionButton(
                "导出文件",
                enable = deviceConnected && selectedFile != null && !isBusy,
                painter = Icon.Outlined.Download
            ) {
                showOptionDialog(FileOptionDialogType.Download)
            }
            OptionButton(
                "导入文件",
                enable = deviceConnected && selectedFile?.type == FileType.DIR && !isBusy,
                painter = Icon.Outlined.Upload
            ) {
                showOptionDialog(FileOptionDialogType.Upload)
            }
            OptionButton(
                "删除",
                enable = deviceConnected && selectedFile != null && !isBusy,
                painter = Icon.Outlined.TrashCan
            ) {
                showOptionDialog(FileOptionDialogType.Delete)
            }
            OptionButton(
                "挂载root",
                enable = deviceConnected && !isBusy,
                painter = Icon.Outlined.AdminPanelSettings,
                onClick = onRemountRoot
            )
            OptionButton(
                "刷新全部",
                enable = deviceConnected && !isBusy,
                painter = Icon.Outlined.Sync,
                onClick = onRefreshRoot
            )
        }
        BusyProcessView(modifier = Modifier.align(Alignment.CenterEnd).size(24.dp), isBusy = isBusy)
    }
}
