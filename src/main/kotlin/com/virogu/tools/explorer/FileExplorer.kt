@file:Suppress("GrazieInspection")

package com.virogu.tools.explorer

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.virogu.bean.AdbDevice
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

    fun createDir(device: AdbDevice, path: String, newFile: String)

    fun createFile(device: AdbDevice, path: String, newFile: String)

    fun deleteFile(device: AdbDevice, file: FileInfoItem, onDeleted: suspend () -> Unit = {})

    fun getChild(fileInfo: FileInfoItem): List<FileItem>

    fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>)

    fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File)

}