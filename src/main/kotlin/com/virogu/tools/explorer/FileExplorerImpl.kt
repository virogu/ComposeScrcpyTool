package com.virogu.tools.explorer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.virogu.bean.AdbDevice
import com.virogu.bean.FileInfo
import com.virogu.bean.FileType
import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.init.InitTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.regex.Pattern

class FileExplorerImpl(
    private val initTool: InitTool,
    private val deviceConnectTool: DeviceConnectTool,
    private val progressTool: ProgressTool,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val jobsMutex = Mutex()
    private val fileMapMutex = Mutex()

    private val fileChildMap: SnapshotStateMap<String, List<FileInfo>> = mutableStateMapOf()
    private val loadingJobs = mutableMapOf<String, Job>()

    val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val expandedMap = mutableStateMapOf<String, Boolean>()

    init {
        start()
    }

    private fun start() {
        scope.launch {
            initTool.initStateFlow.first {
                it.success
            }
            deviceConnectTool.currentSelectedDevice.onEach {
                expandedMap.clear()
                cancelAllJob()
                refresh(it)
            }.launchIn(scope)
        }
    }

    private fun refresh(device: AdbDevice?) {
        fileMapLock {
            it.clear()
        }
        //withLock(FileInfo.ROOT.path) {
        //    if (device == null) {
        //        return@withLock
        //    }
        //    refreshFileChild(device, FileInfo.ROOT.path)
        //}
    }

    fun getChild(fileInfo: FileInfo): List<FileInfo> {
        val device = deviceConnectTool.currentSelectedDevice.value
        if (device == null) {
            return listOf(FileInfo(name = "未连接设备", type = FileType.ERROR))
        } else if (!device.isOnline) {
            return listOf(FileInfo(name = "设备已离线", type = FileType.ERROR))
        }

        if (fileInfo.type != FileType.DIR) {
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
            return listOf(FileInfo("Loading...", type = FileType.TIPS))
        }
        withLock(path) {
            val device = deviceConnectTool.currentSelectedDevice.value ?: return@withLock
            refreshFileChild(device, path)
        }
        return listOf(FileInfo("Loading...", type = FileType.TIPS))
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
            val files: List<FileInfo> = it.parseToFiles(path)
            delay(10)
            fileMapLock { map ->
                map[path] = files
            }
        }.onFailure {
            println("refresh path fail. $it")
        }
    }

    private fun String.parseToFiles(parent: String): List<FileInfo> {
        val lines = trim().split("\n")
        if (lines.isEmpty()) {
            return emptyList()
        }
        if (Pattern.compile(".*\\$parent.*Permission denied.*").matcher(lines.first()).find()) {
            //println("^(.*)?${parent}(.*)?Permission denied(.*)?$ find")
            return listOf(FileInfo(name = lines.first(), type = FileType.ERROR))
        }
        try {
            val files = lines.mapNotNull { line ->
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
                FileInfo(name, parent, "${parent}/${name}", type, size, modificationTime, permissions)
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
                isBusy.emit(true)
                try {
                    block()
                } catch (_: Throwable) {
                } finally {
                    isBusy.emit(false)
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
            block(loadingJobs)
        }
    }

    private fun <T> fileMapLock(
        block: suspend CoroutineScope.(SnapshotStateMap<String, List<FileInfo>>) -> T
    ) = runBlocking {
        fileMapMutex.withLock {
            block(fileChildMap)
        }
    }

    private fun cancelAllJob() {
        jobLock {
            it.forEach { (k, v) ->
                v.cancel()
            }
            it.clear()
        }
    }

}