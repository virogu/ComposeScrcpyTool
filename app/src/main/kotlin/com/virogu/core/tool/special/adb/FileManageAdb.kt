package com.virogu.core.tool.special.adb

import com.virogu.core.bean.*
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.special.FileManage
import java.io.File
import java.util.regex.Pattern

class FileManageAdb(private val progressTool: ProgressTool) : FileManage {

    override suspend fun restartWithRoot(device: DeviceInfo): String = buildString {
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
    override suspend fun refreshPath(
        device: DeviceInfo,
        path: String
    ): Result<List<FileItem>> = progressTool.exec(
        "adb", "-s", device.serial, "shell",
        "ls", "-h", "-g", "-L", path
    ).map {
        val lines = it.trim().split("\n")
        val files: List<FileItem> = if (lines.isEmpty()) {
            emptyList()
        } else if (Pattern.compile(".*\\$path.*Permission denied.*").matcher(lines.first()).find()) {
            //println("^(.*)?${parent}(.*)?Permission denied(.*)?$ find")
            listOf(FileTipsItem.Error(path, lines.first()))
        } else {
            lines.parseToFiles(path)
        }
        files
    }

    override suspend fun createDir(
        device: DeviceInfo, dir: String, newFile: String
    ): Result<String> = progressTool.exec(
        "adb", "-s", device.serial, "shell", "mkdir '${dir}/${newFile}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun createFile(
        device: DeviceInfo, dir: String, newFile: String
    ): Result<String> = progressTool.exec(
        "adb", "-s", device.serial, "shell", "touch '${dir}/${newFile}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun deleteFile(
        device: DeviceInfo, fileItem: FileInfoItem
    ): Result<String> = progressTool.exec(
        "adb", "-s", device.serial, "shell", "rm -r '${fileItem.path}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun getFileDetail(
        device: DeviceInfo, fileItem: FileInfoItem
    ): String = buildString {
        appendLine("路径: ${fileItem.path}")
        append("权限: ${fileItem.permissions}")
        append("  ")
        append("类型: ${fileItem.type.name}")
        append("  ")
        append("大小: ${fileItem.size}")
        appendLine()
        appendLine("修改时间: ${fileItem.modificationTime}")
        progressTool.exec(
            "adb", "-s", device.serial,
            "shell", "md5sum", fileItem.path
        ).onSuccess {
            it.replace("\\s+".toRegex(), " ").split(" ").let { l ->
                if (l.size == 2) {
                    appendLine("MD5: ${l[0]}")
                } else {
                    appendLine("MD5: $it")
                }
            }
        }.onFailure {
            appendLine("获取MD5信息失败 ${it.localizedMessage}")
        }
        progressTool.exec(
            "adb", "-s", device.serial,
            "shell", "sha1sum", fileItem.path
        ).onSuccess {
            it.replace("\\s+".toRegex(), " ").split(" ").let { l ->
                if (l.size == 2) {
                    appendLine("SHA1: ${l[0]}")
                } else {
                    appendLine("SHA1: $it")
                }
            }
        }.onFailure {
            appendLine("获取SHA1信息失败 ${it.localizedMessage}")
        }
    }

    override suspend fun pullFile(
        device: DeviceInfo, fromFile: List<FileInfoItem>, toLocalFile: File
    ): String = buildString {
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

    override suspend fun pushFile(
        device: DeviceInfo, toFile: FileInfoItem, fromLocalFiles: List<File>
    ): String = buildString {
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

    override suspend fun chmod(
        device: DeviceInfo, fileInfo: FileInfoItem, permission: String
    ): String = buildString {
        progressTool.exec(
            "adb", "-s", device.serial, "shell",
            "chmod", permission, fileInfo.path,
            showLog = true
        ).onSuccess {
            if (it.isNotBlank()) {
                appendLine(it)
            } else {
                appendLine("chmod $permission ${fileInfo.path} success")
            }
        }.onFailure {
            appendLine("chmod $permission ${fileInfo.path} fail, ${it.localizedMessage}")
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

}