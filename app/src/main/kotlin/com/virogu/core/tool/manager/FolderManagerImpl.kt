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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class FolderManagerImpl(
    private val initTool: InitTool,
    deviceScan: DeviceScan,
) : FolderManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val jobsMutex = Mutex()
    private val fileMapMutex = Mutex()

    private val fileChildMap: SnapshotStateMap<String, List<FileItem>> = mutableStateMapOf()
    private val expandedMap = mutableStateMapOf<String, Boolean>()
    private val loadingJobs = mutableMapOf<String, Job>()

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
                refresh(null)
            }.launchIn(scope)
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
        withLock("restartWithRoot") {
            val device = currentDevice ?: return@withLock
            val s = device.folderAbility.remount()
            if (s.isNotEmpty()) {
                tipsFlow.emit(s)
            }
        }
    }

    override fun createDir(path: String, newFile: String) {
        val tag = "create dir $newFile in $path"
        println(tag)
        withLock(tag) {
            val device = currentDevice ?: return@withLock
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
        withLock(tag) {
            val device = currentDevice ?: return@withLock
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
        withLock(tag) {
            val device = currentDevice ?: return@withLock
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
        val existJob = jobLock {
            it.containsKey(path)
        }
        if (existJob) {
            return listOf(FileTipsItem.Info(path, "Loading..."))
        }
        withLock(path) {
            val device = currentDevice ?: return@withLock
            refreshFileChild(device, path)
        }
        return listOf(FileTipsItem.Info(path, "Loading..."))
    }

    override fun getFileDetails(fileInfo: FileInfoItem) {
        withLock("get file detail info") {
            val device = currentDevice ?: return@withLock
            val s = device.folderAbility.getFileDetail(fileInfo)
            tipsFlow.emit(s)
        }
    }

    override fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File) {
        withLock("pull file") {
            val device = currentDevice ?: return@withLock
            if (!toLocalFile.isDirectory) {
                tipsFlow.emit("[${toLocalFile.absolutePath}]不是目录")
                return@withLock
            }
            if (!toLocalFile.exists()) {
                tipsFlow.emit("[${toLocalFile.absolutePath}]不存在")
                return@withLock
            }
            val s = device.folderAbility.pullFile(fromFile, toLocalFile)
            tipsFlow.emit(s)
        }
    }

    override fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>) {
        withLock("push file") {
            val device = currentDevice ?: return@withLock
            if (!toFile.isDirectory) {
                tipsFlow.emit("[${toFile.path}]不是目录")
                return@withLock
            }
            val s = device.folderAbility.pushFile(toFile, fromLocalFiles)
            tipsFlow.emit(s)
            refresh(toFile.path)
        }
    }

    override fun chmod(fileInfo: FileInfoItem, permission: String) {
        withLock("chmod file") {
            val device = currentDevice ?: return@withLock
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

    private fun withLock(tag: String, block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            mutex.withLock {
                //isBusy.emit(true)
                try {
                    block()
                } catch (_: Throwable) {
                } finally {
                    // isBusy.emit(false)
                }
            }
        }.also { job ->
            jobLock {
                it[tag]?.cancel()
                it[tag] = job
            }
            job.invokeOnCompletion {
                jobLock {
                    it.remove(tag)
                }
            }
        }
    }

    private fun <T> jobLock(block: suspend CoroutineScope.(MutableMap<String, Job>) -> T) = runBlocking {
        jobsMutex.withLock {
            block(loadingJobs).also {
                isBusy.emit(mutex.isLocked)
            }
        }
    }

    private fun <T> fileMapLock(
        block: suspend CoroutineScope.(SnapshotStateMap<String, List<FileItem>>) -> T
    ) = runBlocking {
        fileMapMutex.withLock {
            block(fileChildMap)
        }
    }

    private fun cancelAllJob() {
        jobLock {
            it.forEach { (_, v) ->
                v.cancel()
            }
            it.clear()
        }
    }

}