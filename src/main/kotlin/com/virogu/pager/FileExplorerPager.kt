@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.virogu.bean.*
import com.virogu.tools.Tools
import com.virogu.tools.explorer.FileExplorer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import theme.Purple200
import theme.materialColors
import java.net.URI
import kotlin.io.path.toPath

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

    val selectFile by rememberUpdatedState { file: FileInfoItem? ->
        currentSelect = file
    }
    val getExpended by rememberUpdatedState { file: FileInfoItem ->
        fileExplorer.expandedMap[file.path] ?: false
    }
    val setExpended by rememberUpdatedState { file: FileInfoItem, expand: Boolean ->
        fileExplorer.expandedMap[file.path] = expand
    }
    val getChildFiles by rememberUpdatedState { file: FileInfoItem ->
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

    val createNewFolder: () -> Unit by rememberUpdatedState label@{
        if (currentDevice.isOffline || currentSelect?.type != FileType.DIR) {
            return@label
        }
        showNewFolderDialog.value = true
    }
    val createNewFile: () -> Unit by rememberUpdatedState label@{
        if (currentDevice.isOffline || currentSelect?.type != FileType.DIR) {
            return@label
        }
        showNewFileDialog.value = true
    }
    val downloadFile: () -> Unit by rememberUpdatedState label@{
        if (currentDevice.isOffline || currentSelect == null) {
            return@label
        }
        showDownloadFileDialog.value = true
    }
    val uploadFile: () -> Unit by rememberUpdatedState label@{
        if (currentDevice.isOffline || currentSelect == null) {
            return@label
        }
        showUploadFileDialog.value = true
    }
    val deleteFile: () -> Unit by rememberUpdatedState label@{
        if (currentDevice.isOffline || currentSelect == null) {
            return@label
        }
        showDeleteDialog.value = true
    }

    val refresh: (FileInfoItem?) -> Unit by rememberUpdatedState label@{
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
                Modifier.padding(horizontal = 16.dp).height(40.dp), currentDevice, tools
            )
            ToolBarView(
                tools = tools,
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
                    Modifier.fillMaxHeight().weight(1f),
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
        TipsView(fileExplorer.tipsFlow)
    }
    NewFolderDialog(showNewFolderDialog, currentSelect, fileExplorer)
    NewFileDialog(showNewFileDialog, currentSelect, fileExplorer)
    FileDownloadDialog(showDownloadFileDialog, currentSelect, fileExplorer)
    FileUploadDialog(showUploadFileDialog, currentSelect, fileExplorer)
    DeleteFileConfirmDialog(showDeleteDialog, currentSelect, fileExplorer, selectFile)
}

@Composable
private fun ColumnScope.SelectDeviceView(
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
        }.align(Alignment.CenterHorizontally),
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

@Composable
private fun ToolBarView(
    tools: Tools,
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
    val fileExplorer = tools.fileExplorer
    val isBusy by fileExplorer.isBusy.collectAsState()
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(35.dp)) {
        Row(Modifier.align(Alignment.CenterStart), Arrangement.spacedBy(8.dp)) {
            OptionButtonView(
                "新建文件夹",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_new_folder.svg"
            ) {
                createNewFolder()
            }
            OptionButtonView(
                "新建文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_new_file.svg"
            ) {
                createNewFile()
            }
            OptionButtonView(
                "导出文件",
                enable = deviceConnected && currentSelect != null,
                resourcePath = "icons/ic_download.svg"
            ) {
                downloadFile()
            }
            OptionButtonView(
                "导入文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_upload.svg"
            ) {
                uploadFile()
            }
            OptionButtonView(
                "删除",
                enable = deviceConnected && currentSelect != null,
                resourcePath = "icons/ic_delete.svg"
            ) {
                deleteFile()
            }
            OptionButtonView(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionButtonView(
    description: String, enable: Boolean, resourcePath: String, onClick: () -> Unit
) {
    val click by rememberUpdatedState(onClick)
    val modifier = Modifier.fillMaxHeight().aspectRatio(1f)
    val shape = RoundedCornerShape(8.dp)
    val colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
    val contentPadding = PaddingValues(6.dp)
    val iconModifier = Modifier
    // wrap button in BoxWithTooltip
    TooltipArea(
        tooltip = {
            Card(elevation = 4.dp) {
                Text(text = description, modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 500, // in milliseconds
    ) {
        Button(
            onClick = {
                click()
            },
            enabled = enable,
            modifier = modifier,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
            elevation = null
        ) {
            Icon(modifier = iconModifier, painter = painterResource(resourcePath), contentDescription = description)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BoxScope.TipsView(
    tipsFlow: SharedFlow<String>
) {
    var tips by remember {
        mutableStateOf("")
    }
    var showTips by remember {
        mutableStateOf(false)
    }
    var mouseEnter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tipsFlow.onEach {
            tips = it.trim()
            showTips = true
        }.launchIn(this)
    }

    LaunchedEffect(showTips, mouseEnter) {
        if (!mouseEnter && showTips) {
            delay(5000)
            showTips = false
        }
    }
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.BottomCenter),
        visible = showTips,
        enter = fadeIn() + expandIn(initialSize = { IntSize(it.width, 0) }),
        exit = shrinkOut(targetSize = { IntSize(it.width, 0) }) + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .onPointerEvent(PointerEventType.Enter) { mouseEnter = true }
                .onPointerEvent(PointerEventType.Exit) { mouseEnter = false },
            border = if (mouseEnter) BorderStroke(1.dp, materialColors.primary) else null,
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Filled.Info, "info")
                Text(tips, Modifier.weight(1f))
                Button(
                    onClick = { showTips = false },
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    contentPadding = PaddingValues(4.dp),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Close, "关闭")
                }
            }
        }
    }
}

private val ERROR_COLOR = Color(0xFFDA4C3F)
private val TIPS_COLOR = Purple200.copy(alpha = 0.8f)

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
            item {
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

        is FileTipsItem -> item {
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
                ContextMenuItem("新建文件夹") {
                    createNewFolder()
                }.also(::add)
                ContextMenuItem("新建文件") {
                    createNewFile()
                }.also(::add)
                ContextMenuItem("导入文件") {
                    uploadFile()
                }.also(::add)
            }
            ContextMenuItem("导出文件") {
                downloadFile()
            }.also(::add)
            ContextMenuItem("删除") {
                deleteFile()
            }.also(::add)
            ContextMenuItem("刷新") {
                refresh(fileInfo)
            }.also(::add)
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
                    }.onExternalDrag(onDrag = {
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
                is FileTipsItem.Error -> ERROR_COLOR
                is FileTipsItem.Info -> TIPS_COLOR
            }, modifier = modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1
        )
    }
}