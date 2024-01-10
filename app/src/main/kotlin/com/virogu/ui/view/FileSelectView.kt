package com.virogu.ui.view

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.onExternalDrag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import theme.FileFolder
import theme.Icon
import theme.textFieldContentPadding
import views.OutlinedTextField
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.toPath
import kotlin.math.roundToInt

/**
 * @author Virogu
 * @since 2022-08-31 16:16
 **/

private val projectRootPath = File("./").absoluteFile

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileSelectView(
    window: ComposeWindow,
    text: String,
    fileChooserType: Int,
    modifier: Modifier = Modifier.height(48.dp),
    filesFilter: Array<String> = emptyArray(),
    multiSelectionEnabled: Boolean = false,
    defaultPath: String = "",
    onFileSelected: (selectedFiles: Array<File>) -> Unit = {}
) {
    val currentOnFileSelected by rememberUpdatedState(onFileSelected)

    var showFileChooser by remember {
        mutableStateOf(false)
    }
    if (showFileChooser) {
        FileChooser(
            defaultPath = defaultPath,
            fileChooserType = fileChooserType,
            filesFilter = filesFilter,
            multiSelectionEnabled = multiSelectionEnabled,
            onClose = {
                showFileChooser = false
            }
        ) {
            currentOnFileSelected(it)
        }
    }
    var dragging: Boolean by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    var focus by remember {
        mutableStateOf(FocusInteraction.Focus())
    }
    LaunchedEffect(dragging) {
        if (dragging) {
            focus = FocusInteraction.Focus()
            interactionSource.emit(focus)
        } else {
            interactionSource.emit(FocusInteraction.Unfocus(focus))
        }
    }
    OutlinedTextField(
        modifier = modifier.onExternalDrag(
            onDragStart = {
                dragging = true
            },
            onDragExit = {
                dragging = false
            },
            onDrop = {
                dragging = false
                it.dragData.also { dragData ->
                    if (dragData !is DragData.FilesList) {
                        return@also
                    }
                    val list = dragData.readFiles().mapNotNull { uri ->
                        try {
                            URI.create(uri).toPath().toFile()
                        } catch (e: Throwable) {
                            null
                        }
                    }.filter { f ->
                        val a1 = when (fileChooserType) {
                            JFileChooser.FILES_ONLY -> {
                                f.isFile
                            }

                            JFileChooser.DIRECTORIES_ONLY -> {
                                f.isDirectory
                            }

                            else -> {
                                true
                            }
                        }
                        val a2 = if (filesFilter.isEmpty()) {
                            true
                        } else {
                            f.isDirectory || (f.isFile && f.extension in filesFilter)
                        }
                        a1 && a2
                    }
                    if (multiSelectionEnabled) {
                        currentOnFileSelected(list.toTypedArray())
                    } else {
                        if (list.size == 1) {
                            currentOnFileSelected(list.toTypedArray())
                        }
                    }
                }
            }
        ),
        readOnly = true,
        singleLine = true,
        value = text,
        onValueChange = {},
        interactionSource = interactionSource,
        trailingIcon = {
            IconButton({
                showFileChooser = true
            }) {
                Icon(
                    painter = Icon.Filled.FileFolder,
                    contentDescription = "选择文件",
                    modifier = Modifier.padding(10.dp),
                    tint = contentColorFor(MaterialTheme.colors.background)
                )
            }
        },
        contentPadding = textFieldContentPadding()
    )
}

@Composable
fun FileChooser(
    defaultPath: String = "",
    title: String = "",
    fileChooserType: Int,
    multiSelectionEnabled: Boolean = false,
    filesFilter: Array<String> = emptyArray(),
    onClose: () -> Unit = {},
    onFileSelected: (selectedFiles: Array<File>) -> Unit,
) {
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val defaultFile = File(defaultPath)
            val f = if (defaultPath.isNotEmpty() && defaultFile.exists()) {
                if (defaultFile.isFile) {
                    defaultFile.parentFile.absoluteFile
                } else {
                    defaultFile.absoluteFile
                }
            } else {
                projectRootPath
            }
            val jFileChooser = JFileChooser(f).apply {
                //设置页面风格
                try {
                    val lookAndFeel = UIManager.getSystemLookAndFeelClassName()
                    UIManager.setLookAndFeel(lookAndFeel)
                    SwingUtilities.updateComponentTreeUI(this)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                dialogTitle = title
                fileSelectionMode = fileChooserType
                isMultiSelectionEnabled = multiSelectionEnabled
                filesFilter.takeIf {
                    it.isNotEmpty()
                }?.apply {
                    val description = filesFilter.joinToString(", ")
                    fileFilter = FileNameExtensionFilter(description, *filesFilter)
                }
            }
            val status = withContext(Dispatchers.Unconfined) {
                jFileChooser.showOpenDialog(null)
            }
            if (status == JFileChooser.APPROVE_OPTION) {
                val files = if (multiSelectionEnabled) {
                    jFileChooser.selectedFiles
                } else {
                    arrayOf(jFileChooser.selectedFile)
                }
                files.takeIf {
                    it.isNotEmpty()
                }?.also(onFileSelected)
                onClose()
            } else {
                onClose()
            }
        }
    }
}

@Composable
fun DropBoxPanel(
    modifier: Modifier,
    window: ComposeWindow,
    onFileDrop: (List<File>) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val component = remember {
        ComposePanel().apply {
            val target = object : DropTarget() {
                override fun drop(event: DropTargetDropEvent) {
                    event.acceptDrop(DnDConstants.ACTION_REFERENCE)
                    val dataFlavors = event.transferable.transferDataFlavors
                    val files = mutableListOf<File>()
                    dataFlavors.forEach {
                        if (it == DataFlavor.javaFileListFlavor) {
                            val list = event.transferable.getTransferData(it) as List<*>
                            files.addAll(list.map { filePath ->
                                File(filePath.toString())
                            })
                        }
                    }
                    if (files.isNotEmpty()) {
                        onFileDrop(files)
                    }
                    event.dropComplete(true)
                }
            }
            dropTarget = target
            isOpaque = false
        }
    }
    val pane = remember {
        window.rootPane
    }
    Box(modifier = modifier.onPlaced {
        val x = it.positionInWindow().x.roundToInt()
        val y = it.positionInWindow().y.roundToInt()
        val width = it.size.width
        val height = it.size.height
        component.setBounds(x, y, width, height)
    }) {
        DisposableEffect(true) {
            pane.add(component)
            onDispose {
                runCatching {
                    pane.remove(component)
                }
            }
        }
        content()
    }
}