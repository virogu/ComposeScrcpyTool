@file:Suppress("GrazieInspection")

package com.virogu.core.tool.manager

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileItem
import com.virogu.core.bean.FileTipsItem
import com.virogu.core.device.Device
import com.virogu.core.tool.init.InitTool
import com.virogu.core.tool.scan.DeviceScan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class FolderManagerImpl(
    private val initTool: InitTool,
    deviceScan: DeviceScan,
) : BaseJobManager(), FolderManager {
    private val fileMapMutex = Mutex()

    private val fileChildMap: SnapshotStateMap<String, List<FileItem>> = mutableStateMapOf()
    private val expandedMap = mutableStateMapOf<String, Boolean>()

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tipsFlow = MutableSharedFlow<String>()

    private val selectedOnlineDevice = deviceScan.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    init {
        start()
    }

    private fun start() {
        scope.launch {
            initTool.waitStart()
            selectedOnlineDevice.onEach {
                expandedMap.clear()
                cancelAllJob()
                isBusy.emit(false)
                refresh(null)
            }.launchIn(this)
        }
    }

    override fun emitTips(tips: String) {
        scope.launch {
            tipsFlow.emit(tips)
        }
    }

    override fun changeExpanded(path: String, expanded: Boolean) {
        if (expanded) {
            expandedMap[path] = true
        } else {
            closeChild(path)
        }
    }

    private fun closeChild(path: String) {
        fileChildMap[path]?.forEach {
            if (it is FileInfoItem && it.isDirectory) {
                closeChild(it.path)
            }
        }
        expandedMap[path] = false
    }

    override fun getExpanded(path: String): Boolean {
        //println("getExpanded $path")
        return expandedMap[path] ?: false
    }

    override fun refresh(path: String?) {
        fileMapLock {
            if (path == null) {
                it.clear()
            } else {
                it.remove(path)
            }
        }
    }

    override fun restartWithRoot() {
        startJob("restartWithRoot") {
            val device = currentDevice ?: return@startJob
            val s = device.folderAbility.remount()
            if (s.isNotEmpty()) {
                tipsFlow.emit(s)
            }
        }
    }

    override fun createDir(path: String, newFile: String) {
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

    override fun createFile(path: String, newFile: String) {
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

    override fun deleteFile(file: FileInfoItem, onDeleted: suspend () -> Unit) {
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

    override fun getChild(fileInfo: FileInfoItem): List<FileItem> {
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

    override fun getFileDetails(fileInfo: FileInfoItem) {
        startJob("get file detail info") {
            val device = currentDevice ?: return@startJob
            val s = device.folderAbility.getFileDetail(fileInfo)
            tipsFlow.emit(s)
        }
    }

    override fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File) {
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

    override fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>) {
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

    override fun chmod(fileInfo: FileInfoItem, permission: String) {
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