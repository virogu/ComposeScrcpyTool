@file:Suppress("GrazieInspection")

package com.virogu.tools

import com.virogu.bean.FileInfoItem
import com.virogu.bean.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface FileExplorer {
    val isBusy: MutableStateFlow<Boolean>
    val tipsFlow: SharedFlow<String>

    fun emitTips(tips: String)

    fun changeExpanded(path: String, expanded: Boolean)

    fun getExpanded(path: String): Boolean

    fun refresh(path: String? = null)

    fun restartWithRoot()

    fun createDir(path: String, newFile: String)

    fun createFile(path: String, newFile: String)

    fun deleteFile(file: FileInfoItem, onDeleted: suspend () -> Unit = {})

    fun getChild(fileInfo: FileInfoItem): List<FileItem>

    fun getFileDetails(fileInfo: FileInfoItem)

    fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>)

    fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File)

    fun chmod(fileInfo: FileInfoItem, permission: String)

}