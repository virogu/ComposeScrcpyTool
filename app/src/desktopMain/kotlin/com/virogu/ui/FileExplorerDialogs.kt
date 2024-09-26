@file:Suppress("FunctionName")

package com.virogu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FilePermission
import com.virogu.core.bean.FileType
import com.virogu.core.tool.manager.FolderManager
import com.virogu.ui.view.FileChooser
import theme.MainTheme
import theme.materialColors
import views.modifier.onEnterKey
import javax.swing.JFileChooser

@Composable
fun NewFolderDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FolderManager
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
    fileExplore: FolderManager
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
    fileExplore: FolderManager,
    selectFile: (FileInfoItem?) -> Unit
) {
    if (currentSelect == null) {
        return
    }
    if (show.value) {
        val tips = if (currentSelect.isDirectory) {
            "确定删除目录下的所有目录和文件\n\n${currentSelect.path}"
        } else {
            "确定删除文件\n\n${currentSelect.path}"
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
    fileExplore: FolderManager
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
    fileExplore: FolderManager
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
fun ChmodFileDialog(
    show: MutableState<Boolean>,
    currentSelect: FileInfoItem?,
    fileExplore: FolderManager
) {
    if (currentSelect == null) {
        return
    }
    if (show.value) {
        ShowChmodDialog(
            windowTitle = "更改权限",
            onClose = {
                show.value = false
            },
            defaultPermission = currentSelect.permission,
            onConfirm = label@{
                fileExplore.chmod(currentSelect, permission = it)
            }
        )
    }
}

@Composable
private fun ShowChmodDialog(
    windowTitle: String,
    onClose: () -> Unit,
    defaultPermission: FilePermission = FilePermission(),
    defaultWidth: Dp = TextFieldDefaults.MinWidth.times(2f),
    defaultHeight: Dp = TextFieldDefaults.MinHeight.times(10f),
    onConfirm: (String) -> Unit,
) {
    val close by rememberUpdatedState(onClose)
    val confirm by rememberUpdatedState(onConfirm)

    var permission by remember {
        mutableStateOf(defaultPermission)
    }

    var text by remember(permission) {
        mutableStateOf(permission.value)
    }
    LaunchedEffect(text) {
        val p = FilePermission.parseIntString(text)
        if (p != null) {
            permission = p
        }
    }
    val errorString by remember(text) {
        val matchResults = Regex("[0-7]{4}").findAll(text)
        if (matchResults.count() <= 0) {
            mutableStateOf("""权限格式不正确, 例: 0777 """)
        } else {
            mutableStateOf("")
        }
    }
    val error by remember(errorString) {
        mutableStateOf(errorString.isNotEmpty())
    }
    val confirmEnable by remember(errorString, text) {
        mutableStateOf(errorString.isEmpty() && text.isNotEmpty())
    }
    val state = rememberDialogState(
        size = DpSize(defaultWidth, defaultHeight)
    )
    DialogWindow(
        onCloseRequest = {
            close()
        },
        state = state,
        title = windowTitle,
    ) {
        MainTheme {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val modifier = Modifier.align(Alignment.CenterHorizontally)
                OutlinedTextField(
                    modifier = modifier,
                    value = text,
                    onValueChange = {
                        text = it.filter { s -> s.isDigit() }.take(4)
                    },
                    isError = error,
                    label = {
                        if (errorString.isNotEmpty()) {
                            Text(errorString)
                        } else {
                            Text(permission.desc)
                        }
                    },
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val m1 = Modifier.weight(1f).align(Alignment.CenterHorizontally)
                    val textAlign = TextAlign.Center
                    Row(Modifier.fillMaxWidth()) {
                        Text("", m1, textAlign = textAlign)
                        Text("所有者", m1, textAlign = textAlign)
                        Text("组", m1, textAlign = textAlign)
                        Text("其他", m1, textAlign = textAlign)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("读", m1, textAlign = textAlign)
                        Checkbox(permission.owner.readable, {
                            permission = permission.copy(owner = permission.owner.copy(readable = it))
                        }, modifier = m1)
                        Checkbox(permission.group.readable, {
                            permission = permission.copy(group = permission.group.copy(readable = it))
                        }, modifier = m1)
                        Checkbox(permission.other.readable, {
                            permission = permission.copy(other = permission.other.copy(readable = it))
                        }, modifier = m1)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("写", m1, textAlign = textAlign)
                        Checkbox(permission.owner.writeable, {
                            permission = permission.copy(owner = permission.owner.copy(writeable = it))
                        }, modifier = m1)
                        Checkbox(permission.group.writeable, {
                            permission = permission.copy(group = permission.group.copy(writeable = it))
                        }, modifier = m1)
                        Checkbox(permission.other.writeable, {
                            permission = permission.copy(other = permission.other.copy(writeable = it))
                        }, modifier = m1)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("执行", m1, textAlign = textAlign)
                        Checkbox(permission.owner.executable, {
                            permission = permission.copy(owner = permission.owner.copy(executable = it))
                        }, modifier = m1)
                        Checkbox(permission.group.executable, {
                            permission = permission.copy(group = permission.group.copy(executable = it))
                        }, modifier = m1)
                        Checkbox(permission.other.executable, {
                            permission = permission.copy(other = permission.other.copy(executable = it))
                        }, modifier = m1)
                    }
                    Spacer(
                        Modifier.padding(32.dp, 16.dp).fillMaxWidth().height(1.dp)
                            .background(materialColors.onBackground.copy(alpha = 0.7f))
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text("setuid", m1, textAlign = textAlign)
                        Text("setgid", m1, textAlign = textAlign)
                        Text("sticky", m1, textAlign = textAlign)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Checkbox(permission.special.setUid, {
                            permission = permission.copy(special = permission.special.copy(setUid = it))
                        }, modifier = m1)
                        Checkbox(permission.special.setGid, {
                            permission = permission.copy(special = permission.special.copy(setGid = it))
                        }, modifier = m1)
                        Checkbox(permission.special.sticky, {
                            permission = permission.copy(special = permission.special.copy(sticky = it))
                        }, modifier = m1)
                    }
                }
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
    DialogWindow(
        onCloseRequest = {
            close()
        },
        title = windowTitle,
        state = state
    ) {
        MainTheme {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val modifier = Modifier.align(Alignment.CenterHorizontally)
                Text(inputTips, modifier.padding(8.dp))
                OutlinedTextField(
                    modifier = modifier.padding(horizontal = 16.dp).focusRequester(focusRequester).onEnterKey {
                        if (confirmEnable) {
                            onConfirm(text)
                            close()
                        }
                    },
                    value = text,
                    onValueChange = {
                        text = it.trim().take(50)
                    },
                    isError = error,
                    label = {
                        if (errorString.isNotEmpty()) {
                            Text(errorString)
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
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
    DialogWindow(
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