@file:Suppress("FunctionName")

package com.virogu.pager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import com.virogu.bean.FileInfoItem
import com.virogu.bean.FileType
import com.virogu.pager.view.FileChooser
import com.virogu.tools.explorer.FileExplorer
import theme.MainTheme
import theme.materialColors
import javax.swing.JFileChooser

@Composable
fun NewFolderDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FileExplorer
) {
    if (currentSelect?.type != FileType.DIR) {
        return
    }
    if (show.value) {
        FileNameInputDialog(
            windowTitle = "新建文件夹",
            inputTips = "请输入文件夹名称",
            onClose = {
                show.value = false
            },
            onConfirm = label@{
                fileExplore.createDir(path = currentSelect.path, newFile = it)
            }
        )
    }
}

@Composable
fun NewFileDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FileExplorer
) {
    if (currentSelect?.type != FileType.DIR) {
        return
    }
    if (show.value) {
        FileNameInputDialog(
            windowTitle = "新建文件",
            inputTips = "请输入文件名称",
            onClose = {
                show.value = false
            },
            onConfirm = label@{
                fileExplore.createFile(path = currentSelect.path, newFile = it)
            }
        )
    }
}

@Composable
fun DeleteFileConfirmDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FileExplorer,
    selectFile: (FileInfoItem?) -> Unit
) {
    if (currentSelect == null) {
        return
    }
    if (show.value) {
        val tips = if (currentSelect.isDirectory) {
            "确定删除目录\n\n${currentSelect.path}\n\n以及下面所有目录和文件吗"
        } else {
            "确定删除文件\n\n${currentSelect.path}\n\n吗"
        }
        CommonConfirmDialog(
            windowTitle = "提示",
            message = tips,
            onClose = {
                show.value = false
            },
            onConfirm = label@{
                fileExplore.deleteFile(currentSelect, onDeleted = {
                    selectFile(null)
                })
            }
        )
    }
}

@Composable
fun FileDownloadDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FileExplorer
) {
    if (currentSelect == null) {
        return
    }
    if (show.value) {
        FileChooser(
            title = "选择要导出的目录",
            fileChooserType = JFileChooser.DIRECTORIES_ONLY,
            onClose = {
                show.value = false
            },
            multiSelectionEnabled = false
        ) {
            val target = it.firstOrNull() ?: return@FileChooser
            fileExplore.pullFile(listOf(currentSelect), target)
        }
    }
}

@Composable
fun FileUploadDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FileExplorer
) {
    if (currentSelect == null) {
        return
    }
    if (show.value) {
        FileChooser(
            title = "选择要导入的文件或目录",
            fileChooserType = JFileChooser.FILES_AND_DIRECTORIES,
            onClose = {
                show.value = false
            },
            multiSelectionEnabled = true
        ) {
            fileExplore.pushFile(currentSelect, it.toList())
        }
    }
}


@Composable
private fun FileNameInputDialog(
    windowTitle: String,
    inputTips: String,
    onClose: () -> Unit,
    defaultWidth: Dp = TextFieldDefaults.MinWidth.times(1.5f),
    defaultHeight: Dp = TextFieldDefaults.MinHeight.times(6f),
    onConfirm: (String) -> Unit,
) {
    val close by rememberUpdatedState(onClose)
    val confirm by rememberUpdatedState(onConfirm)

    var text by remember {
        mutableStateOf("")
    }
    val errorString by remember(text) {
        val matchResults = Regex("[/\\\\:*?\"<>|]").findAll(text)
        if (matchResults.count() <= 0) {
            mutableStateOf("")
        } else {
            mutableStateOf("""不允许的字符 / \ : * ? " < > | """)
        }
    }
    val error by remember(errorString) {
        mutableStateOf(errorString.isNotEmpty())
    }
    val confirmEnable by remember(errorString, text) {
        mutableStateOf(errorString.isEmpty() && text.isNotEmpty())
    }

    val state = rememberDialogState()
    state.size = DpSize(defaultWidth, defaultHeight)
    Dialog(
        onCloseRequest = {
            close()
        },
        title = windowTitle,
        state = state
    ) {
        MainTheme {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val modifier = Modifier.align(Alignment.CenterHorizontally)
                Text(inputTips, modifier.padding(8.dp))
                OutlinedTextField(
                    modifier = modifier.padding(horizontal = 16.dp),
                    value = text,
                    onValueChange = {
                        text = it.take(50)
                    },
                    isError = error,
                    label = {
                        if (errorString.isNotEmpty()) {
                            Text(errorString)
                        }
                    }
                )
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Button(
                        onClick = {
                            confirm(text)
                            close()
                        },
                        enabled = confirmEnable,
                        border = BorderStroke(1.dp, materialColors.primary),
                        colors = ButtonDefaults.buttonColors(backgroundColor = materialColors.primary.copy(alpha = 0.5f))
                    ) {
                        Text("确定")
                    }
                    Button(
                        onClick = {
                            close()
                        },
                        border = BorderStroke(1.dp, materialColors.primary),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
                    ) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonConfirmDialog(
    windowTitle: String,
    message: String,
    onClose: () -> Unit,
    defaultWidth: Dp = 400.dp,
    defaultHeight: Dp = 300.dp,
    fontSize: TextUnit = 16.sp,
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit,
) {
    val close by rememberUpdatedState(onClose)
    val cancel by rememberUpdatedState(onCancel)
    val confirm by rememberUpdatedState(onConfirm)

    val state = rememberDialogState()
    state.size = DpSize(defaultWidth, defaultHeight)
    Dialog(
        onCloseRequest = {
            close()
        },
        title = windowTitle,
        state = state
    ) {
        MainTheme {
            Column {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Text(
                        text = message,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        fontSize = fontSize
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Button(
                        onClick = {
                            confirm()
                            close()
                        },
                        border = BorderStroke(1.dp, materialColors.primary),
                        colors = ButtonDefaults.buttonColors(backgroundColor = materialColors.primary.copy(alpha = 0.5f))
                    ) {
                        Text("确定")
                    }
                    Button(
                        onClick = {
                            cancel()
                            close()
                        },
                        border = BorderStroke(1.dp, materialColors.primary),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
                    ) {
                        Text("取消")
                    }
                }
            }
        }
    }
}