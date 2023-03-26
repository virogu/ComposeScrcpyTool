@file:Suppress("GrazieInspection")

package com.virogu.tools.explorer

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.virogu.bean.FileInfoItem
import com.virogu.bean.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface FileExplorer {
    val isBusy: MutableStateFlow<Boolean>
    val expandedMap: SnapshotStateMap<String, Boolean>
    val tipsFlow: SharedFlow<String>

    fun refresh(path: String? = null)

    fun restartWithRoot()

    fun createDir(path: String, newFile: String)

    fun createFile(path: String, newFile: String)

    fun deleteFile(file: FileInfoItem, onDeleted: suspend () -> Unit = {})

    fun getChild(fileInfo: FileInfoItem): List<FileItem>

    fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>)

    fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File)

}