@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.virogu.bean.AdbDevice
import com.virogu.bean.FileInfo
import com.virogu.bean.FileType
import com.virogu.tools.Tools
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import theme.Purple200
import theme.materialColors

@Composable
fun FileExplorerPager(
    tools: Tools,
    fileListState: LazyListState,
) {
    val fileExplorer = tools.fileExplorer
    val scrollAdapter = rememberScrollbarAdapter(fileListState)
    val currentDevice = tools.deviceConnectTool.currentSelectedDevice.collectAsState()
    val currentSelect: MutableState<FileInfo?> = remember(currentDevice.value) {
        mutableStateOf(null)
    }

    val selectFile by rememberUpdatedState { file: FileInfo? ->
        currentSelect.value = file
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

    val showNewFolderDialog = remember(currentDevice.value) {
        mutableStateOf(false)
    }
    val showNewFileDialog = remember(currentDevice.value) {
        mutableStateOf(false)
    }
    val showDownloadFileDialog = remember(currentDevice.value) {
        mutableStateOf(false)
    }
    val showUploadFileDialog = remember(currentDevice.value) {
        mutableStateOf(false)
    }
    val showDeleteDialog = remember(currentDevice.value) {
        mutableStateOf(false)
    }

    val deviceDisconnect: () -> Boolean = {
        currentDevice.value?.isOnline != true
    }

    val createNewFolder: () -> Unit = label@{
        if (deviceDisconnect() || currentSelect.value?.type != FileType.DIR) {
            return@label
        }
        showNewFolderDialog.value = true
    }
    val createNewFile: () -> Unit = label@{
        if (deviceDisconnect() || currentSelect.value?.type != FileType.DIR) {
            return@label
        }
        showNewFileDialog.value = true
    }
    val downloadFile: () -> Unit = label@{
        if (deviceDisconnect() || currentSelect.value == null) {
            return@label
        }
        showDownloadFileDialog.value = true
    }
    val uploadFile: () -> Unit = label@{
        if (deviceDisconnect() || currentSelect.value == null) {
            return@label
        }
        showUploadFileDialog.value = true
    }
    val deleteFile: () -> Unit = label@{
        if (deviceDisconnect() || currentSelect.value == null) {
            return@label
        }
        showDeleteDialog.value = true
    }

    val refresh: (FileInfo?) -> Unit = label@{
        if (deviceDisconnect()) {
            return@label
        }
        if (it == null) {
            selectFile(null)
            fileExplorer.refresh()
        } else {
            fileExplorer.refresh(it.path)
        }
    }

    Box {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectDeviceView(
                Modifier.padding(horizontal = 16.dp).height(40.dp), currentDevice, tools
            )
            ToolBarView(
                tools = tools,
                currentSelect = currentSelect.value,
                currentDevice = currentDevice.value,
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
                    getChildFiles(FileInfo.ROOT).forEach {
                        FileView(
                            fileInfo = it,
                            currentSelect = currentSelect.value,
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
        TipsView(fileExplorer.tipsFlow)
    }
    NewFolderDialog(showNewFolderDialog, currentDevice.value, currentSelect.value, fileExplorer)
    NewFileDialog(showNewFileDialog, currentDevice.value, currentSelect.value, fileExplorer)
    FileDownloadDialog(showDownloadFileDialog, currentDevice.value, currentSelect.value, fileExplorer)
    FileUploadDialog(showUploadFileDialog, currentDevice.value, currentSelect.value, fileExplorer)
    DeleteFileConfirmDialog(showDeleteDialog, currentDevice.value, currentSelect.value, fileExplorer, selectFile)
}

@Composable
private fun ColumnScope.SelectDeviceView(
    modifier: Modifier = Modifier, currentDevice: State<AdbDevice?>, tools: Tools
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
                text = currentDevice.value?.showName.orEmpty(),
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
    currentSelect: FileInfo?,
    currentDevice: AdbDevice?,
    createNewFolder: () -> Unit,
    createNewFile: () -> Unit,
    downloadFile: () -> Unit,
    uploadFile: () -> Unit,
    deleteFile: () -> Unit,
    refresh: (FileInfo?) -> Unit,
) {

    val onCreateNewFolder by rememberUpdatedState(createNewFolder)
    val onCreateNewFile by rememberUpdatedState(createNewFile)
    val onDownloadFile by rememberUpdatedState(downloadFile)
    val onUploadFile by rememberUpdatedState(uploadFile)
    val onDeleteFile by rememberUpdatedState(deleteFile)
    val onRefresh by rememberUpdatedState(refresh)

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
                onCreateNewFolder()
            }
            OptionButtonView(
                "新建文件",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_new_file.svg"
            ) {
                onCreateNewFile()
            }
            OptionButtonView(
                "导出",
                enable = deviceConnected && currentSelect != null,
                resourcePath = "icons/ic_download.svg"
            ) {
                onDownloadFile()
            }
            OptionButtonView(
                "导入",
                enable = deviceConnected && currentSelect?.type == FileType.DIR,
                resourcePath = "icons/ic_upload.svg"
            ) {
                onUploadFile()
            }
            OptionButtonView(
                "删除",
                enable = deviceConnected && currentSelect != null,
                resourcePath = "icons/ic_delete.svg"
            ) {
                onDeleteFile()
            }
            OptionButtonView(
                "刷新",
                enable = deviceConnected,
                resourcePath = "icons/ic_sync.svg"
            ) {
                onRefresh(null)
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
    Button(
        onClick = {
            click()
        }, enabled = enable, modifier = modifier, shape = shape, colors = colors, contentPadding = contentPadding
    ) {
        Icon(modifier = iconModifier, painter = painterResource(resourcePath), contentDescription = description)
    }
}

@Composable
private fun BoxScope.TipsView(
    tipsFlow: SharedFlow<String>
) {
    val tipsState by tipsFlow.collectAsState("")
    var tips by remember(tipsState) {
        mutableStateOf(tipsState)
    }
    LaunchedEffect(tips) {
        if (tips.isNotEmpty()) {
            delay(5000)
            tips = ""
        }
    }
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.BottomCenter),
        visible = tips.isNotEmpty()
    ) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp).height(50.dp)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                Text(tips, Modifier.weight(1f).align(Alignment.CenterVertically))
                Icon(
                    Icons.Default.Close,
                    "关闭",
                    modifier = Modifier.size(30.dp).clickable {
                        tips = ""
                    }.align(Alignment.CenterVertically),
                    tint = contentColorFor(MaterialTheme.colors.background)
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
    selectFile: (FileInfo?) -> Unit,
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
        Card(modifier = Modifier.height(40.dp).clickable { }.run {
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
        }, backgroundColor = backgroundColor.value, elevation = 0.dp
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

                    else -> {
                        Spacer(iconModifier)
                        Spacer(iconModifier)
                    }
                }
                Text(
                    text = fileInfo.name, color = when (fileInfo.type) {
                        FileType.ERROR -> ERROR_COLOR
                        FileType.TIPS -> TIPS_COLOR
                        else -> Color.Unspecified
                    }, modifier = modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1
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