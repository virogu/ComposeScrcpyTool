@file:Suppress("GrazieInspection")

package com.virogu.tools.explorer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.virogu.bean.*
import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.init.InitTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.regex.Pattern

class FileExplorerImpl(
    private val initTool: InitTool,
    deviceConnectTool: DeviceConnectTool,
    private val progressTool: ProgressTool,
) : FileExplorer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val jobsMutex = Mutex()
    private val fileMapMutex = Mutex()

    private val fileChildMap: SnapshotStateMap<String, List<FileItem>> = mutableStateMapOf()
    private val expandedMap = mutableStateMapOf<String, Boolean>()
    private val loadingJobs = mutableMapOf<String, Job>()

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tipsFlow = MutableSharedFlow<String>()

    private val selectedOnlineDevice = deviceConnectTool.currentSelectedDevice.map {
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
            initTool.initStateFlow.first {
                it.success
            }
            selectedOnlineDevice.onEach {
                expandedMap.clear()
                cancelAllJob()
                refresh(null)
            }.launchIn(scope)
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
            val s = buildString {
                progressTool.exec("adb", "-s", device.serial, "root").onSuccess {
                    if (it.isNotEmpty()) {
                        appendLine(it)
                    }
                }.onFailure {
                    it.printStackTrace()
                    appendLine("restart with root fail")
                }
                progressTool.exec("adb", "-s", device.serial, "remount").onSuccess {
                    if (it.isNotEmpty()) {
                        appendLine(it)
                    }
                }.onFailure {
                    it.printStackTrace()
                    appendLine("restart with root fail")
                }
            }
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
            progressTool.exec(
                "adb", "-s", device.serial, "shell", "mkdir '${path}/${newFile}'",
                showLog = true
            ).onSuccess {
                if (it.isNotEmpty()) {
                    println(it)
                    tipsFlow.emit(it)
                } else {
                    refresh(path)
                }
            }.onFailure {
                it.printStackTrace()
                tipsFlow.emit("create dir $newFile in $path fail")
            }
        }
    }

    override fun createFile(path: String, newFile: String) {
        val tag = "create file $newFile in $path"
        println(tag)
        withLock(tag) {
            val device = currentDevice ?: return@withLock
            progressTool.exec(
                "adb", "-s", device.serial, "shell",
                //"-p",
                "touch '${path}/${newFile}'",
                showLog = true
            ).onSuccess {
                if (it.isNotEmpty()) {
                    println(it)
                    tipsFlow.emit(it)
                } else {
                    refresh(path)
                }
            }.onFailure {
                it.printStackTrace()
                tipsFlow.emit("create file $newFile in $path fail")
            }
        }
    }

    override fun deleteFile(file: FileInfoItem, onDeleted: suspend () -> Unit) {
        val path = file.path
        val tag = "delete $path"
        withLock(tag) {
            val device = currentDevice ?: return@withLock
            if (path.count { it == '/' } <= 1) {
                tipsFlow.emit("Are you sure you want rm -r $path?\nPlease delete it by yourself")
                return@withLock
            }
            progressTool.exec(
                "adb", "-s", device.serial, "shell",
                "rm -r '$path'",
                showLog = true
            ).onSuccess {
                if (it.isNotEmpty()) {
                    println(it)
                    tipsFlow.emit(it)
                } else {
                    refresh(file.parentPath)
                    onDeleted()
                }
            }.onFailure {
                it.printStackTrace()
                tipsFlow.emit("delete $path fail")
            }
        }
    }

    override fun getChild(fileInfo: FileInfoItem): List<FileItem> {
        //println("getChild ${fileInfo.path}")
        if (currentDevice == null) {
            return listOf(FileTipsItem.Error("未连接设备"))
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
            return listOf(FileTipsItem.Info("Loading..."))
        }
        withLock(path) {
            val device = currentDevice ?: return@withLock
            refreshFileChild(device, path)
        }
        return listOf(FileTipsItem.Info("Loading..."))
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
            val s = buildString {
                fromFile.forEach { f ->
                    progressTool.exec(
                        "adb", "-s", device.serial,
                        "pull", f.path, toLocalFile.absolutePath,
                        showLog = true
                    ).onSuccess {
                        appendLine(it)
                    }.onFailure {
                        appendLine("pull file [${f.path}] fail, ${it.localizedMessage}")
                    }
                }
            }
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
            val s = buildString {
                fromLocalFiles.forEach { f ->
                    val args = if (f.isDirectory) {
                        arrayOf("${f.absolutePath}\\.", "${toFile.path}/${f.name}/.")
                    } else {
                        arrayOf(f.absolutePath, "${toFile.path}/${f.name}")
                    }
                    progressTool.exec(
                        "adb", "-s", device.serial,
                        "push", *args,
                        showLog = true
                    ).onSuccess {
                        appendLine(it)
                    }.onFailure {
                        appendLine("push file [${f.absolutePath}] fail, ${it.localizedMessage}")
                    }
                }
            }
            tipsFlow.emit(s)
            refresh(toFile.path)
        }
    }

    /**
     * what to show:
     * -a  all files including .hidden    -b  escape nongraphic chars
     * -c  use ctime for timestamps       -d  directory, not contents
     * -i  inode number                   -p  put a '/' after dir names
     * -q  unprintable chars as '?'       -s  storage used (1024 byte units)
     * -u  use access time for timestamps -A  list all files but . and ..
     * -H  follow command line symlinks   -L  follow symlinks
     * -R  recursively list in subdirs    -F  append /dir *exe @sym |FIFO
     * -Z  security context
     *
     * output formats:
     * -1  list one file per line         -C  columns (sorted vertically)
     * -g  like -l but no owner           -h  human readable sizes
     * -l  long (show full details)       -m  comma separated
     * -n  like -l but numeric uid/gid    -o  like -l but no group
     * -x  columns (horizontal sort)      -ll long with nanoseconds (--full-time)
     *
     * sorting (default is alphabetical):
     * -f  unsorted    -r  reverse    -t  timestamp    -S  size
     * --color  device=yellow  symlink=turquoise/red  dir=blue  socket=purple
     *          files: exe=green  suid=red  suidfile=redback  stickydir=greenback
     *          =auto means detect if output is a tty.
     */
    private suspend fun refreshFileChild(device: AdbDevice, path: String) {
        progressTool.exec(
            "adb", "-s", device.serial, "shell",
            "ls", "-h", "-g", "-L", path
        ).onSuccess {
            delay(10)
            //println(it)
            val lines = it.trim().split("\n")
            val files: List<FileItem> = if (lines.isEmpty()) {
                emptyList()
            } else if (Pattern.compile(".*\\$path.*Permission denied.*").matcher(lines.first()).find()) {
                //println("^(.*)?${parent}(.*)?Permission denied(.*)?$ find")
                listOf(FileTipsItem.Error(lines.first()))
            } else {
                lines.parseToFiles(path)
            }
            delay(10)
            fileMapLock { map ->
                map[path] = files
            }
        }.onFailure {
            println("refresh path fail. $it")
        }
    }

    private fun List<String>.parseToFiles(parent: String): List<FileInfoItem> {
        if (this.isEmpty()) {
            return emptyList()
        }
        try {
            val files = this.mapNotNull { line ->
                val matcher = Pattern.compile(
                    "^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+\\s+)?(.*)$"
                ).matcher(line.trim())
                if (!matcher.find()) {
                    //println("Failed to parse: $line")
                    return@mapNotNull null
                }
                val permissions = matcher.group(1).orEmpty()
                if (permissions.startsWith("l")) {
                    return@mapNotNull null
                }
                val type = when {
                    permissions.startsWith("-") -> FileType.FILE
                    permissions.startsWith("d", true) -> FileType.DIR
                    permissions.startsWith("l", true) -> FileType.LINK
                    else -> FileType.OTHER
                }
                val size = matcher.group(4).orEmpty().ifEmpty {
                    "0"
                }.also {
                    if (!it.first().isDigit()) {
                        return@mapNotNull null
                    }
                }.plus("B")
                val isLowerFormat = matcher.group(5).length < 8 //10  1970-01-01
                val modificationTime = if (isLowerFormat) {
                    "${matcher.group(5)} ${matcher.group(6)} ${matcher.group(7)}"
                } else {
                    "${matcher.group(5)} ${matcher.group(6)}"
                }
                val name = if (isLowerFormat) {
                    matcher.group(8).orEmpty()
                } else {
                    "${matcher.group(7).orEmpty()}${matcher.group(8).orEmpty()}"
                }
                FileInfoItem(name, parent, "${parent}/${name}", type, size, modificationTime, permissions)
            }
            return files.sortedBy {
                it.type.sortIndex
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return emptyList()
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