package com.virogu.pager.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

/**
 * @author Virogu
 * @since 2022-08-31 16:16
 **/

private val projectRootPath = File("./").absoluteFile

@Composable
fun FileSelectView(
    window: ComposeWindow,
    text: String,
    fileChooserType: Int,
    modifier: Modifier = Modifier.height(40.dp),
    filesFilter: Array<String> = emptyArray(),
    multiSelectionEnabled: Boolean = false,
    defaultPath: String = "",
    onFileSelected: (selectedFiles: Array<File>) -> Unit = {}
) {
    val currentOnFileSelected by rememberUpdatedState(onFileSelected)

    val showFileChooser = {
        showFileChooser(
            defaultPath = defaultPath,
            fileChooserType = fileChooserType,
            filesFilter = filesFilter,
            multiSelectionEnabled = multiSelectionEnabled,
        ) {
            currentOnFileSelected(it)
        }
    }

    DropBoxPanel(modifier, window = window, onFileDrop = {
        val list = it.filter { f ->
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
    }) {
        val borderStroke = animateBorderStrokeAsState()
        Box(
            modifier = modifier.border(borderStroke.value, TextFieldDefaults.OutlinedTextFieldShape),
        ) {
            SelectionContainer(
                Modifier.fillMaxSize().align(Alignment.Center)
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    modifier = Modifier.wrapContentHeight().fillMaxWidth().align(Alignment.Center).padding(
                        start = 8.dp, end = 8.dp
                    ),
                    textAlign = TextAlign.Start
                )
            }
            Box(
                Modifier.fillMaxHeight().clickable {
                    showFileChooser()
                }.aspectRatio(1f).padding().align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource("icons/ic_folder_fill.svg"),
                    contentDescription = "选择文件",
                    modifier = Modifier.fillMaxHeight(0.6f).aspectRatio(1f).align(Alignment.Center),
                    tint = contentColorFor(MaterialTheme.colors.background)
                )
            }
        }
    }
}

fun showFileChooser(
    defaultPath: String = "",
    fileChooserType: Int,
    multiSelectionEnabled: Boolean = false,
    filesFilter: Array<String> = emptyArray(),
    onFileSelected: (selectedFiles: Array<File>) -> Unit,
) {
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
    JFileChooser(f).apply {
        //设置页面风格
        try {
            val lookAndFeel = UIManager.getSystemLookAndFeelClassName()
            UIManager.setLookAndFeel(lookAndFeel)
            SwingUtilities.updateComponentTreeUI(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        fileSelectionMode = fileChooserType
        isMultiSelectionEnabled = multiSelectionEnabled
        filesFilter.takeIf {
            it.isNotEmpty()
        }?.apply {
            val description = filesFilter.joinToString(", ")
            fileFilter = FileNameExtensionFilter(description, *filesFilter)
        }
        val status = showOpenDialog(null)
        if (status == JFileChooser.APPROVE_OPTION) {
            val files = if (multiSelectionEnabled) {
                selectedFiles
            } else {
                arrayOf(selectedFile)
            }
            files.takeIf {
                it.isNotEmpty()
            }?.also(onFileSelected)
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