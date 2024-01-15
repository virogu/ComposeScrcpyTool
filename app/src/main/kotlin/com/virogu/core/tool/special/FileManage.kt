package com.virogu.core.tool.special

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileItem
import java.io.File


interface FileManage {

    suspend fun restartWithRoot(device: DeviceInfo): String

    suspend fun refreshPath(device: DeviceInfo, path: String): Result<List<FileItem>>

    suspend fun createDir(device: DeviceInfo, dir: String, newFile: String): Result<String>

    suspend fun createFile(device: DeviceInfo, dir: String, newFile: String): Result<String>

    suspend fun deleteFile(device: DeviceInfo, fileItem: FileInfoItem): Result<String>

    suspend fun getFileDetail(device: DeviceInfo, fileItem: FileInfoItem): String

    suspend fun pullFile(device: DeviceInfo, fromFile: List<FileInfoItem>, toLocalFile: File): String

    suspend fun pushFile(device: DeviceInfo, toFile: FileInfoItem, fromLocalFiles: List<File>): String

    suspend fun chmod(device: DeviceInfo, fileInfo: FileInfoItem, permission: String): String
}