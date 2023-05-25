@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virogu.bean.*
import com.virogu.pager.view.OptionButton
import com.virogu.pager.view.SelectDeviceView
import com.virogu.pager.view.TipsView
import com.virogu.tools.Tools
import com.virogu.tools.explorer.FileExplorer
import theme.materialColors
import java.net.URI
import kotlin.io.path.toPath

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileExplorerPager(
    tools: Tools,
    fileListState: LazyListState,
) {
    val fileExplorer = tools.fileExplorer
    val scrollAdapter = rememberScrollbarAdapter(fileListState)
    val currentDevice by tools.deviceConnectTool.currentSelectedDevice.collectAsState()
    var currentSelect: FileInfoItem? by remember(currentDevice) {
        mutableStateOf(null)
    }

    val selectFile = { file: FileInfoItem? ->
        currentSelect = file
    }
    val getExpended: (FileInfoItem) -> Boolean = { file: FileInfoItem ->
        if (file.isDirectory) {
            fileExplorer.getExpanded(file.path)
        } else {
            false
        }
    }
    val setExpended = { file: FileInfoItem, expand: Boolean ->
        fileExplorer.changeExpanded(file.path, expand)
    }
    val getChildFiles = { file: FileInfoItem ->
        fileExplorer.getChild(file)
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

    val createNewFolder: () -> Unit = label@{
        if (currentDevice.isOffline || currentSelect?.type != FileType.DIR) {
            return@label
        }
        showNewFolderDialog.value = true
    }
    val createNewFile: () -> Unit = label@{
        if (currentDevice.isOffline || currentSelect?.type != FileType.DIR) {
            return@label
        }
        showNewFileDialog.value = true
    }
    val downloadFile: () -> Unit = label@{
        if (currentDevice.isOffline || currentSelect == null) {
            return@label
        }
        showDownloadFileDialog.value = true
    }
    val uploadFile: () -> Unit = label@{
        if (currentDevice.isOffline || currentSelect == null) {
            return@label
        }
        showUploadFileDialog.value = true
    }
    val deleteFile: () -> Unit = label@{
        if (currentDevice.isOffline || currentSelect == null) {
            return@label
        }
        showDeleteDialog.value = true
    }

    val refresh: (FileInfoItem?) -> Unit = label@{
        if (currentDevice.isOffline) {
            return@label
        }
        if (it == null) {
            selectFile(null)
            fileExplorer.refresh(null)
        } else if (it.isDirectory) {
            fileExplorer.refresh(it.path)
        } else {
            fileExplorer.refresh(it.parentPath)
        }
    }

    Box {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectDeviceView(
                Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp).height(40.dp),
                currentDevice,
                tools
            )
            ToolBarView(
                fileExplorer = tools.fileExplorer,
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
                            fileExplorer = fileExplorer,
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
        TipsView(Modifier.align(Alignment.BottomCenter), fileExplorer.tipsFlow)
    }
    NewFolderDialog(showNewFolderDialog, currentSelect, fileExplorer)
    NewFileDialog(showNewFileDialog, currentSelect, fileExplorer)
    FileDownloadDialog(showDownloadFileDialog, currentSelect, fileExplorer)
    FileUploadDialog(showUploadFileDialog, currentSelect, fileExplorer)
    DeleteFileConfirmDialog(showDeleteDialog, currentSelect, fileExplorer, selectFile)
}

@Composable
private fun ToolBarView(
    fileExplorer: FileExplorer,
    currentSelect: FileInfoItem?,
    currentDevice: AdbDevice?,
    createNewFolder: () -> Unit,
    createNewFile: () -> Unit,
    downloadFile: () -> Unit,
    uploadFile: () -> Unit,
    deleteFile: () -> Unit,
    refresh: (FileInfoItem?) -> Unit,
) {
    val deviceConnected = currentDevice?.isOnline == true
    val isBusy by fileExplorer.isBusy.collectAsState()
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(35.dp)) {
        Row(Modifier.align(Alignment.CenterStart), Arrangement.spacedBy(8.dp)) {
            OptionButton(
                "新建文件夹",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_new_folder.svg"
            ) {
                createNewFolder()
            }
            OptionButton(
                "新建文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_new_file.svg"
            ) {
                createNewFile()
            }
            OptionButton(
                "导出文件",
                enable = deviceConnected && currentSelect != null,
                resourcePath = "icons/ic_download.svg"
            ) {
                downloadFile()
            }
            OptionButton(
                "导入文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_upload.svg"
            ) {
                uploadFile()
            }
            OptionButton(
                "删除",
                enable = deviceConnected && currentSelect != null,
                resourcePath = "icons/ic_delete.svg"
            ) {
                deleteFile()
            }
            OptionButton(
                "以root模式连接",
                enable = deviceConnected,
                resourcePath = "icons/ic_admin_panel_settings.svg"
            ) {
                fileExplorer.restartWithRoot()
            }
            OptionButton(
                "刷新",
                enable = deviceConnected,
                resourcePath = "icons/ic_sync.svg"
            ) {
                refresh(null)
            }
        }
        if (isBusy) {
            val infiniteTransition by rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            Icon(
                modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).rotate(infiniteTransition),
                painter = painterResource("icons/ic_clock_loader.svg"),
                contentDescription = "运行状态"
            )
        }
    }
}

private fun LazyListScope.FileView(
    fileExplorer: FileExplorer,
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
) {
    when (fileItem) {
        is FileInfoItem -> {
            val currentExpanded = getExpended(fileItem)
            item(key = fileItem.path) {
                FileInfoItemView(
                    fileExplorer,
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
                )
            }
            if (currentExpanded) {
                getChildFiles(fileItem).forEach {
                    FileView(
                        fileExplorer = fileExplorer,
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
                        refresh = refresh,
                        deleteFile = deleteFile,
                    )
                }
            }
        }

        is FileTipsItem -> item(key = fileItem.msg) {
            FileTipsItemView(fileItem, level)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FileInfoItemView(
    fileExplorer: FileExplorer,
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
) {
    val selected = remember(fileInfo.path, currentSelect) {
        mutableStateOf(currentSelect == fileInfo)
    }
    val refreshPath by rememberUpdatedState {
        refresh(fileInfo)
    }
    val primaryColor = materialColors.primary.copy(alpha = 0.5f)
    var mouseEnter by remember { mutableStateOf(false) }
    val backgroundColor by remember(selected.value, mouseEnter) {
        val c = if (selected.value) {
            primaryColor.copy(alpha = 0.5f)
        } else if (mouseEnter) {
            primaryColor.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        }
        mutableStateOf(c)
    }
    val contextMenuState = remember { ContextMenuState() }

    LaunchedEffect(contextMenuState.status) {
        if (contextMenuState.status is ContextMenuState.Status.Open) {
            selectFile(fileInfo)
        }
    }
    ContextMenuArea(state = contextMenuState, items = {
        mutableListOf<ContextMenuItem>().apply {
            if (fileInfo.isDirectory) {
                ContextMenuItem("新建文件夹", createNewFolder).also(::add)
                ContextMenuItem("新建文件", createNewFile).also(::add)
                ContextMenuItem("导入文件", uploadFile).also(::add)
            }
            ContextMenuItem("导出文件", downloadFile).also(::add)
            ContextMenuItem("删除", deleteFile).also(::add)
            ContextMenuItem("刷新", refreshPath).also(::add)
        }
    }) {
        Card(modifier = Modifier.height(40.dp).onPointerEvent(PointerEventType.Enter) {
            mouseEnter = true
        }.onPointerEvent(PointerEventType.Exit) {
            mouseEnter = false
        }.run {
            when (fileInfo.type) {
                FileType.DIR -> {
                    onClick(
                        onDoubleClick = {
                            setExpended(fileInfo, !currentExpanded)
                        }, onClick = {

                        }
                    ).onPointerEvent(PointerEventType.Press) {
                        selectFile(fileInfo)
                    }.onExternalDrag(onDragStart = {
                        mouseEnter = true
                    }, onDragExit = {
                        mouseEnter = false
                    }) {
                        it.dragData.also { dragData ->
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
                            fileExplorer.pushFile(fileInfo, files)
                            println("onDrop $files")
                        }
                    }
                }

                FileType.FILE -> {
                    onPointerEvent(PointerEventType.Press) { selectFile(fileInfo) }
                }

                else -> this
            }
        }, backgroundColor = backgroundColor, elevation = 0.dp) {
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
                                        Icons.Filled.KeyboardArrowRight
                                    }, contentDescription = fileInfo.name
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