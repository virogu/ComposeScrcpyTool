package com.virogu.core.device.ability

import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileItem
import java.io.File

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:35
 **/
interface DeviceAbilityFolder {
    suspend fun remount(): String

    suspend fun refreshPath(path: String): Result<List<FileItem>>

    suspend fun createDir(dir: String, newFile: String): Result<String>

    suspend fun createFile(dir: String, newFile: String): Result<String>

    suspend fun deleteFile(fileItem: FileInfoItem): Result<String>

    suspend fun getFileDetail(fileItem: FileInfoItem): String

    suspend fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File): String

    suspend fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>): String

    suspend fun chmod(fileInfo: FileInfoItem, permission: String): String
}