/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.viewModelScope
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileItem
import com.virogu.core.bean.FileTipsItem
import com.virogu.core.device.Device
import com.virogu.core.tool.connect.DeviceConnect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File

/**
 * @author Virogu
 * @since 2024-09-11 上午11:10
 **/
class FolderViewModel : BaseJobViewModel() {
    private val deviceConnect by DI.global.instance<DeviceConnect>()

    private val fileMapMutex = Mutex()
    private val fileChildMap: SnapshotStateMap<String, List<FileItem>> = mutableStateMapOf()
    private val expandedMap = mutableStateMapOf<String, Boolean>()

    val tipsFlow = MutableSharedFlow<String>()

    private val selectedOnlineDevice = deviceConnect.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    init {
        start()
    }

    private fun start() {
        selectedOnlineDevice.onEach {
            expandedMap.clear()
            cancelAllJob()
            mIsBusy.emit(false)
            refresh(null)
        }.launchIn(viewModelScope)
    }

    fun emitTips(tips: String) {
        viewModelScope.launch {
            tipsFlow.emit(tips)
        }
    }

    fun changeExpanded(path: String, expanded: Boolean) {
        if (expanded) {
            expandedMap[path] = true
        } else {
            fileMapLock {
                fun closeChild(p: String) {
                    it[p]?.forEach { item ->
                        if (item is FileInfoItem && item.isDirectory) {
                            closeChild(item.path)
                        }
                    }
                    expandedMap[p] = false
                }
                closeChild(path)
            }
        }
    }

    fun getExpanded(path: String): Boolean {
        //println("getExpanded $path")
        return expandedMap[path] ?: false
    }

    fun refresh(path: String?) {
        fileMapLock {
            if (path == null) {
                it.clear()
            } else {
                fun remove(p: String) {
                    it.remove(p)?.forEach { item ->
                        if (item is FileInfoItem && item.isDirectory) {
                            remove(item.path)
                        }
                    }
                }
                remove(path)
            }
        }
    }

    fun restartWithRoot() {
        startJob("restartWithRoot") {
            val device = currentDevice ?: return@startJob
            val s = device.folderAbility.remount()
            if (s.isNotEmpty()) {
                tipsFlow.emit(s)
            }
        }
    }

    fun createDir(path: String, newFile: String) {
        val tag = "create dir $newFile in $path"
        println(tag)
        startJob(tag) {
            val device = currentDevice ?: return@startJob
            device.folderAbility.createDir(path, newFile).onSuccess {
                refresh(path)
            }.onFailure {
                it.printStackTrace()
                tipsFlow.emit(it.localizedMessage)
            }
        }
    }

    fun createFile(path: String, newFile: String) {
        val tag = "create file $newFile in $path"
        println(tag)
        startJob(tag) {
            val device = currentDevice ?: return@startJob
            device.folderAbility.createFile(path, newFile).onSuccess {
                refresh(path)
            }.onFailure {
                it.printStackTrace()
                tipsFlow.emit(it.localizedMessage)
            }
        }
    }

    fun deleteFile(file: FileInfoItem, onDeleted: suspend () -> Unit) {
        val path = file.path
        val tag = "delete $path"
        startJob(tag) {
            val device = currentDevice ?: return@startJob
            device.folderAbility.deleteFile(file).onSuccess {
                refresh(file.parentPath)
                onDeleted()
            }.onFailure {
                it.printStackTrace()
                tipsFlow.emit(it.localizedMessage)
            }
        }
    }

    fun getChild(fileInfo: FileInfoItem): List<FileItem> {
        //println("getChild ${fileInfo.path}")
        if (currentDevice == null) {
            return listOf(FileTipsItem.Error(fileInfo.path, "未连接设备"))
        }

        if (!fileInfo.isDirectory) {
            return emptyList()
        }
        val path = fileInfo.path
        val children = fileMapLock {
            it[path]
        }
        if (children != null) {
            return children
        }
        if (getJob(path) != null) {
            return listOf(FileTipsItem.Info(path, "Loading..."))
        }
        startJob(path) {
            val device = currentDevice ?: return@startJob
            refreshFileChild(device, path)
        }
        return listOf(FileTipsItem.Info(path, "Loading..."))
    }

    fun getFileDetails(fileInfo: FileInfoItem) {
        startJob("get file detail info") {
            val device = currentDevice ?: return@startJob
            val s = device.folderAbility.getFileDetail(fileInfo)
            tipsFlow.emit(s)
        }
    }

    fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File) {
        startJob("pull file") {
            val device = currentDevice ?: return@startJob
            if (!toLocalFile.isDirectory) {
                tipsFlow.emit("[${toLocalFile.absolutePath}]不是目录")
                return@startJob
            }
            if (!toLocalFile.exists()) {
                tipsFlow.emit("[${toLocalFile.absolutePath}]不存在")
                return@startJob
            }
            val s = device.folderAbility.pullFile(fromFile, toLocalFile)
            tipsFlow.emit(s)
        }
    }

    fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>) {
        startJob("push file") {
            val device = currentDevice ?: return@startJob
            if (!toFile.isDirectory) {
                tipsFlow.emit("[${toFile.path}]不是目录")
                return@startJob
            }
            val s = device.folderAbility.pushFile(toFile, fromLocalFiles)
            tipsFlow.emit(s)
            refresh(toFile.path)
        }
    }

    fun chmod(fileInfo: FileInfoItem, permission: String) {
        startJob("chmod file") {
            val device = currentDevice ?: return@startJob
            val s = device.folderAbility.chmod(fileInfo, permission)
            tipsFlow.emit(s)
            refresh(fileInfo.parentPath)
        }
    }

    private suspend fun refreshFileChild(device: Device, path: String) {
        val list = device.folderAbility.refreshPath(path).getOrNull().orEmpty()
        fileMapLock { map ->
            map[path] = list
        }
    }

    private fun <T> fileMapLock(
        block: suspend CoroutineScope.(SnapshotStateMap<String, List<FileItem>>) -> T
    ) = runBlocking {
        fileMapMutex.withLock {
            block(fileChildMap)
        }
    }

}