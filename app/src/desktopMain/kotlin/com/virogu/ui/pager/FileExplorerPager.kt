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

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileItem
import com.virogu.core.bean.FileTipsItem
import com.virogu.core.bean.FileType
import com.virogu.core.device.Device
import com.virogu.core.tool.Tools
import com.virogu.core.viewmodel.FolderViewModel
import com.virogu.ui.*
import com.virogu.ui.view.BusyProcessView
import com.virogu.ui.view.OptionButton
import com.virogu.ui.view.SelectDeviceView
import com.virogu.ui.view.TipsView
import theme.*
import java.awt.datatransfer.StringSelection
import java.net.URI
import kotlin.io.path.toPath

@Composable
fun FileExplorerPager(
    tools: Tools,
    fileListState: LazyListState,
    model: FolderViewModel = viewModel { FolderViewModel() }
) {
    val scrollAdapter = rememberScrollbarAdapter(fileListState)
    val currentDevice by tools.deviceConnect.currentSelectedDevice.collectAsState()
    var currentSelect: FileInfoItem? by remember(currentDevice) {
        mutableStateOf(null)
    }

    val selectFile = { file: FileInfoItem? ->
        currentSelect = file
    }
    val getExpended: (FileInfoItem) -> Boolean = { file: FileInfoItem ->
        if (file.isDirectory) {
            model.getExpanded(file.path)
        } else {
            false
        }
    }
    val setExpended = { file: FileInfoItem, expand: Boolean ->
        model.changeExpanded(file.path, expand)
    }
    val getChildFiles = { file: FileInfoItem ->
        model.getChild(file)
    }

    val showNewFolderDialog = remember(currentDevice, currentSelect) {
        mutableStateOf(false)
    }
    val showNewFileDialog = remember(currentDevice, currentSelect) {
        mutableStateOf(false)
    }
    val showDownloadFileDialog = remember(currentDevice, currentSelect) {
        mutableStateOf(false)
    }
    val showUploadFileDialog = remember(currentDevice, currentSelect) {
        mutableStateOf(false)
    }
    val showDeleteDialog = remember(currentDevice, currentSelect) {
        mutableStateOf(false)
    }
    val showChmodDialog = remember(currentDevice, currentSelect) {
        mutableStateOf(false)
    }

    val createNewFolder: () -> Unit = label@{
        if (currentSelect?.type != FileType.DIR) {
            return@label
        }
        showNewFolderDialog.value = true
    }
    val createNewFile: () -> Unit = label@{
        if (currentSelect?.type != FileType.DIR) {
            return@label
        }
        showNewFileDialog.value = true
    }
    val downloadFile: () -> Unit = label@{
        if (currentSelect == null) {
            return@label
        }
        showDownloadFileDialog.value = true
    }
    val uploadFile: () -> Unit = label@{
        if (currentSelect == null) {
            return@label
        }
        showUploadFileDialog.value = true
    }
    val deleteFile: () -> Unit = label@{
        if (currentSelect == null) {
            return@label
        }
        showDeleteDialog.value = true
    }

    val refresh: (FileInfoItem?) -> Unit = label@{
        if (it == null) {
            selectFile(null)
            model.refresh(null)
        } else if (it.isDirectory) {
            model.refresh(it.path)
        } else {
            model.refresh(it.parentPath)
        }
    }

    val chmodFile: () -> Unit = label@{
        if (currentSelect == null) {
            return@label
        }
        showChmodDialog.value = true
    }

    Box {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectDeviceView(
                Modifier.textFieldHeight().align(Alignment.CenterHorizontally).padding(horizontal = 16.dp),
                currentDevice,
                tools.deviceConnect
            )
            ToolBarView(
                viewModel = model,
                currentSelect = currentSelect,
                currentDevice = currentDevice,
                createNewFolder = createNewFolder,
                createNewFile = createNewFile,
                downloadFile = downloadFile,
                uploadFile = uploadFile,
                refresh = refresh,
                deleteFile = deleteFile,
            )
            Row {
                LazyColumn(
                    Modifier.fillMaxHeight().weight(1f).onKeyEvent { event ->
                        if (currentDevice == null) {
                            return@onKeyEvent false
                        }
                        if (event.type != KeyEventType.KeyUp) {
                            return@onKeyEvent false
                        }
                        when (event.key) {
                            Key.F5 -> {
                                refresh(currentSelect)
                                true
                            }

                            Key.Delete -> {
                                deleteFile()
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
                    getChildFiles(FileInfoItem.ROOT).forEach {
                        FileView(
                            viewModel = model,
                            fileItem = it,
                            currentSelect = currentSelect,
                            level = 0,
                            getChildFiles = getChildFiles,
                            selectFile = selectFile,
                            getExpended = getExpended,
                            setExpended = setExpended,
                            createNewFolder = createNewFolder,
                            createNewFile = createNewFile,
                            downloadFile = downloadFile,
                            uploadFile = uploadFile,
                            refresh = refresh,
                            deleteFile = deleteFile,
                            chmodFile = chmodFile,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                VerticalScrollbar(
                    modifier = Modifier,
                    adapter = scrollAdapter,
                    reverseLayout = false,
                )
            }
        }
        TipsView(Modifier.align(Alignment.BottomCenter), model.tipsFlow)
    }
    NewFolderDialog(showNewFolderDialog, currentSelect, model)
    NewFileDialog(showNewFileDialog, currentSelect, model)
    FileDownloadDialog(showDownloadFileDialog, currentSelect, model)
    FileUploadDialog(showUploadFileDialog, currentSelect, model)
    DeleteFileConfirmDialog(showDeleteDialog, currentSelect, selectFile, model)
    ChmodFileDialog(showChmodDialog, currentSelect, model)
}

@Composable
private fun ToolBarView(
    currentSelect: FileInfoItem?,
    currentDevice: Device?,
    createNewFolder: () -> Unit,
    createNewFile: () -> Unit,
    downloadFile: () -> Unit,
    uploadFile: () -> Unit,
    deleteFile: () -> Unit,
    refresh: (FileInfoItem?) -> Unit,
    viewModel: FolderViewModel
) {
    val deviceConnected = currentDevice?.isOnline == true
    val isBusy by viewModel.isBusy.collectAsState()
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(35.dp)) {
        Row(Modifier.align(Alignment.CenterStart), Arrangement.spacedBy(8.dp)) {
            OptionButton(
                "新建文件夹",
                enable = deviceConnected && currentSelect?.type == FileType.DIR && !isBusy,
                painter = Icon.Outlined.FileNewFolder
            ) {
                createNewFolder()
            }
            OptionButton(
                "新建文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR && !isBusy,
                painter = Icon.Outlined.FileNewFile
            ) {
                createNewFile()
            }
            OptionButton(
                "导出文件",
                enable = deviceConnected && currentSelect != null && !isBusy,
                painter = Icon.Outlined.Download
            ) {
                downloadFile()
            }
            OptionButton(
                "导入文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR && !isBusy,
                painter = Icon.Outlined.Upload
            ) {
                uploadFile()
            }
            OptionButton(
                "删除",
                enable = deviceConnected && currentSelect != null && !isBusy,
                painter = Icon.Outlined.TrashCan
            ) {
                deleteFile()
            }
            OptionButton(
                "挂载root",
                enable = deviceConnected && !isBusy,
                painter = Icon.Outlined.AdminPanelSettings
            ) {
                viewModel.restartWithRoot()
            }
            OptionButton(
                "刷新",
                enable = deviceConnected && !isBusy,
                painter = Icon.Outlined.Sync
            ) {
                refresh(null)
            }
        }
        BusyProcessView(modifier = Modifier.align(Alignment.CenterEnd).size(24.dp), isBusy = isBusy)
    }
}

private fun LazyListScope.FileView(
    fileItem: FileItem,
    currentSelect: FileInfoItem?,
    level: Int,
    getChildFiles: (FileInfoItem) -> List<FileItem>,
    selectFile: (FileInfoItem?) -> Unit,
    getExpended: (FileInfoItem) -> Boolean,
    setExpended: (FileInfoItem, Boolean) -> Unit,
    createNewFolder: () -> Unit,
    createNewFile: () -> Unit,
    downloadFile: () -> Unit,
    uploadFile: () -> Unit,
    deleteFile: () -> Unit,
    refresh: (FileInfoItem?) -> Unit,
    chmodFile: () -> Unit,
    viewModel: FolderViewModel,
) {
    when (fileItem) {
        is FileInfoItem -> {
            val currentExpanded = getExpended(fileItem)
            //ohos系统里面文件夹下居然可以存在两个完全相同的文件，这里用文件路径作为key会重复
            //item(key = fileItem.path) {
            item(key = null) {
                FileInfoItemView(
                    viewModel = viewModel,
                    fileInfo = fileItem,
                    currentSelect = currentSelect,
                    level = level,
                    selectFile = selectFile,
                    currentExpanded = currentExpanded,
                    setExpended = setExpended,
                    createNewFolder = createNewFolder,
                    createNewFile = createNewFile,
                    downloadFile = downloadFile,
                    uploadFile = uploadFile,
                    refresh = refresh,
                    deleteFile = deleteFile,
                    chmodFile = chmodFile,
                )
            }
            if (currentExpanded) {
                getChildFiles(fileItem).forEach {
                    FileView(
                        viewModel = viewModel,
                        fileItem = it,
                        currentSelect = currentSelect,
                        level = level + 1,
                        getChildFiles = getChildFiles,
                        selectFile = selectFile,
                        getExpended = getExpended,
                        setExpended = setExpended,
                        createNewFolder = createNewFolder,
                        createNewFile = createNewFile,
                        downloadFile = downloadFile,
                        uploadFile = uploadFile,
                        deleteFile = deleteFile,
                        refresh = refresh,
                        chmodFile = chmodFile,
                    )
                }
            }
        }

        //is FileTipsItem -> item(key = "tips_${fileItem.path}") {
        is FileTipsItem -> item(key = null) {
            FileTipsItemView(fileItem, level)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FileInfoItemView(
    fileInfo: FileInfoItem,
    currentSelect: FileInfoItem?,
    level: Int,
    selectFile: (FileInfoItem?) -> Unit,
    currentExpanded: Boolean,
    setExpended: (FileInfoItem, Boolean) -> Unit,
    createNewFolder: () -> Unit,
    createNewFile: () -> Unit,
    downloadFile: () -> Unit,
    uploadFile: () -> Unit,
    deleteFile: () -> Unit,
    refresh: (FileInfoItem?) -> Unit,
    chmodFile: () -> Unit,
    viewModel: FolderViewModel
) {
    val clipboardManager = LocalClipboardManager.current
    val selected = remember(fileInfo.path, currentSelect) {
        mutableStateOf(currentSelect == fileInfo)
    }
    val refreshPath by rememberUpdatedState {
        refresh(fileInfo)
    }
    val copyName by rememberUpdatedState {
        clipboardManager.setText(AnnotatedString(fileInfo.name))
        viewModel.emitTips("${fileInfo.name} 名称已复制")
    }
    val copyPath by rememberUpdatedState {
        clipboardManager.setText(AnnotatedString(fileInfo.path))
        viewModel.emitTips("${fileInfo.path} 路径已复制")
    }
    val getFileDetails by rememberUpdatedState {
        viewModel.getFileDetails(fileInfo)
    }

    var mouseEnter by remember { mutableStateOf(false) }
    val backgroundColor by rememberItemBackground(selected.value, mouseEnter)
    val contextMenuState = remember { ContextMenuState() }
    val dragAndDropTargetCallback = remember(fileInfo) {
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
                        } catch (e: Throwable) {
                            null
                        }
                    }
                    viewModel.pushFile(fileInfo, files)
                    println("onDrop $files")
                }
                return true
            }
        }
    }

    val onDoubleTap by rememberUpdatedState {
        when (fileInfo.type) {
            FileType.DIR -> {
                setExpended(fileInfo, !currentExpanded)
            }

            else -> {}
        }
    }

    LaunchedEffect(contextMenuState.status) {
        if (contextMenuState.status is ContextMenuState.Status.Open) {
            selectFile(fileInfo)
        }
    }
    ContextMenuArea(state = contextMenuState, items = {
        mutableListOf<ContextMenuItem>().apply {
            ContextMenuItem("刷新", refreshPath).also(::add)
            if (fileInfo.isDirectory) {
                ContextMenuItem("新建文件夹", createNewFolder).also(::add)
                ContextMenuItem("新建文件", createNewFile).also(::add)
                ContextMenuItem("导入文件", uploadFile).also(::add)
            }
            ContextMenuItem("导出文件", downloadFile).also(::add)
            ContextMenuItem("复制名称", copyName).also(::add)
            ContextMenuItem("复制路径", copyPath).also(::add)
            ContextMenuItem("显示详细信息", getFileDetails).also(::add)
            ContextMenuItem("修改权限", chmodFile).also(::add)
            ContextMenuItem("删除", deleteFile).also(::add)
        }
    }) {
        val textMeasurer = rememberTextMeasurer()
        val colors = materialColors
        Card(
            modifier = Modifier.height(40.dp).onPointerEvent(PointerEventType.Enter) {
                mouseEnter = true
            }.onPointerEvent(PointerEventType.Exit) {
                mouseEnter = false
            }.onPointerEvent(PointerEventType.Release) {
                mouseEnter = false
            }.run {
                when (fileInfo.type) {
                    FileType.DIR -> {
                        dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dragAndDropTargetCallback)
                    }

                    else -> this
                }
            }.onPointerEvent(PointerEventType.Press) { selectFile(fileInfo) }.dragAndDropSource(
                drawDragDecoration = {
                    drawRect(
                        color = colors.primary,
                        //topLeft = Offset(x = 0f, y = size.height / 4),
                        size = Size(size.width / 2, size.height)
                    )
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(fileInfo.name),
                        layoutDirection = layoutDirection,
                        density = this
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        color = colors.onSurface,
                        topLeft = Offset(
                            x = (size.width / 2 - textLayoutResult.size.width) / 2,
                            y = (size.height - textLayoutResult.size.height) / 2,
                        )
                    )
                }
            ) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTap()
                    },
                    onLongPress = { offset ->
                        startTransfer(
                            DragAndDropTransferData(
                                transferable = DragAndDropTransferable(
                                    StringSelection(fileInfo.path)
                                ),
                                supportedActions = listOf(
                                    DragAndDropTransferAction.Copy,
                                    DragAndDropTransferAction.Move,
                                    DragAndDropTransferAction.Link,
                                ),
                                dragDecorationOffset = offset,
                                onTransferCompleted = { action ->
                                    when (action) {
                                        DragAndDropTransferAction.Copy -> println("Copied")
                                        DragAndDropTransferAction.Move -> println("Moved")
                                        DragAndDropTransferAction.Link -> println("Linked")
                                        null -> println("Transfer aborted")
                                    }
                                }
                            )
                        )
                    })
            },
            backgroundColor = backgroundColor, elevation = 0.dp
        ) {
            var nameHasVisualOverflow by remember { mutableStateOf(false) }
            TooltipArea(
                tooltip = {
                    if (nameHasVisualOverflow && contextMenuState.status !is ContextMenuState.Status.Open) {
                        Card(elevation = 4.dp) {
                            Text(text = fileInfo.name, modifier = Modifier.padding(8.dp))
                        }
                    }
                },
                delayMillis = 800,
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
                        Spacer(Modifier.width((level * 8).dp))
                        when (fileInfo.type) {
                            FileType.DIR -> {
                                Icon(
                                    modifier = modifier.size(30.dp).clickable {
                                        selectFile(fileInfo)
                                        setExpended(fileInfo, !currentExpanded)
                                    }.padding(5.dp), imageVector = if (currentExpanded) {
                                        Icons.Filled.KeyboardArrowDown
                                    } else {
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                                    }, contentDescription = fileInfo.name
                                )
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileFolder,
                                    contentDescription = fileInfo.name
                                )
                            }

                            FileType.LINK -> {
                                Spacer(iconModifier)
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileLink,
                                    contentDescription = fileInfo.name
                                )
                            }

                            FileType.FILE -> {
                                Spacer(iconModifier)
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileDocument,
                                    contentDescription = fileInfo.name
                                )
                            }

                            FileType.OTHER -> {
                                Spacer(iconModifier)
                                Icon(
                                    modifier = iconModifier,
                                    painter = Icon.Outlined.FileUnknown,
                                    contentDescription = fileInfo.name
                                )
                            }

                        }
                        Text(
                            text = fileInfo.name,
                            color = materialColors.onSurface,
                            modifier = modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = {
                                nameHasVisualOverflow = it.hasVisualOverflow
                            },
                            maxLines = 1
                        )
                    }
                    Text(
                        text = fileInfo.permissions,
                        color = materialColors.onSurface.copy(0.8f),
                        modifier = modifier.weight(2f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = fileInfo.modificationTime,
                        color = materialColors.onSurface.copy(0.8f),
                        modifier = modifier.weight(2f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = fileInfo.size,
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
private fun FileTipsItemView(
    fileTipsItem: FileTipsItem,
    level: Int,
) {
    Row(
        modifier = Modifier.height(40.dp).padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val modifier = Modifier.align(Alignment.CenterVertically)
        Spacer(Modifier.width((level * 8).dp.plus(40.dp)))
        Text(
            text = fileTipsItem.name, color = when (fileTipsItem) {
                is FileTipsItem.Error -> materialColors.error
                is FileTipsItem.Info -> materialColors.primary.copy(alpha = 0.8f)
            }, modifier = modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1
        )
    }
}