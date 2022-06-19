package com.virogu.pager.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.virogu.pager.defaultTextSize
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
    label: String,
    text: String,
    fileChooserType: Int,
    filesFilter: Array<String> = emptyArray(),
    multiSelectionEnabled: Boolean = false,
    buttonText: String = "选择",
    defaultPath: String = "",
    onFileSelected: (selectedFiles: Array<File>) -> Unit = {}
) {
    val showFileChooser = {
        showFileChooser(
            defaultPath = defaultPath,
            fileChooserType = fileChooserType,
            filesFilter = filesFilter,
            multiSelectionEnabled = multiSelectionEnabled,
            onFileSelected = onFileSelected
        )
    }
    Row(Modifier.fillMaxWidth().wrapContentHeight(), Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(80.dp).align(Alignment.CenterVertically)
        )
        DropBoxPanel(Modifier.weight(1f), window = window, onFileDrop = {
            it.filter { f ->
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
            }.also { list ->
                if (multiSelectionEnabled) {
                    onFileSelected(list.toTypedArray())
                } else {
                    if (list.size == 1) {
                        onFileSelected(list.toTypedArray())
                    }
                }
            }
        }) {
            val borderStroke = animateBorderStrokeAsState()
            Box(
                modifier = Modifier.fillMaxWidth().defaultTextSize()
                    .border(borderStroke.value, TextFieldDefaults.OutlinedTextFieldShape),
            ) {
                SelectionContainer {
                    Text(text = text, modifier = Modifier.defaultTextSize().align(Alignment.CenterStart).padding(16.dp))
                }
            }
        }
        TextButton(
            onClick = {
                showFileChooser()
            },
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            Text(buttonText)
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
                    dataFlavors.forEach {
                        if (it == DataFlavor.javaFileListFlavor) {
                            val list = event.transferable.getTransferData(it) as List<*>
                            list.map { filePath ->
                                File(filePath.toString())
                            }.also(onFileDrop)
                        }
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