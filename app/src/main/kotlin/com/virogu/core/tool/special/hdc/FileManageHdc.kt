package com.virogu.core.tool.special.hdc

import com.virogu.core.bean.*
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.special.FileManage
import java.io.File
import java.nio.charset.Charset
import java.util.regex.Pattern

//TODO  对中文目录的各种操作还是有问题，编码的问题，暂时不知道怎么解决
class FileManageHdc(private val progressTool: ProgressTool) : FileManage {
    private val specialCharset = Charset.forName("GBK")
    private val specialEnv = mapOf(
        "LC_ALL" to "zh_CN.GBK"
    )

    override suspend fun restartWithRoot(device: DeviceInfo): String = buildString {
        progressTool.exec("hdc", "-t", device.serial, "target", "mount").onSuccess {
            if (it.isNotEmpty()) {
                appendLine(it)
            }
        }.onFailure {
            it.printStackTrace()
            appendLine("remount fail")
        }
    }

    override suspend fun refreshPath(
        device: DeviceInfo, path: String
    ): Result<List<FileItem>> {
        return progressTool.exec(
            "hdc", "-t", device.serial, "shell",
            "ls", "-h", "-g", "-L", path.ifEmpty { "/" }, consoleLog = true,
            charset = specialCharset
        ).map {
            val lines = it.trim().split("\n")
            val files: List<FileItem> = if (lines.isEmpty()) {
                emptyList()
            } else if (Pattern.compile(".*\\$path.*Permission denied.*").matcher(lines.first()).find()) {
                listOf(FileTipsItem.Error(path, lines.first()))
            } else {
                lines.parseToFiles(path)
            }
            files
        }
    }

    override suspend fun createDir(
        device: DeviceInfo,
        dir: String,
        newFile: String
    ): Result<String> = progressTool.exec(
        "hdc", "-t", device.serial, "shell", "mkdir '${dir}/${newFile}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }


    override suspend fun createFile(
        device: DeviceInfo,
        dir: String,
        newFile: String
    ): Result<String> = progressTool.exec(
        "hdc", "-t", device.serial, "shell", "touch '${dir}/${newFile}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun deleteFile(
        device: DeviceInfo,
        fileItem: FileInfoItem
    ): Result<String> = progressTool.exec(
        "hdc", "-t", device.serial, "shell", "rm -r '${fileItem.path}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun getFileDetail(
        device: DeviceInfo,
        fileItem: FileInfoItem
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
            "hdc", "-t", device.serial,
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
            "hdc", "-t", device.serial,
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
                "hdc", "-t", device.serial,
                "file", "recv", "-a", f.path, toLocalFile.absolutePath,
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
                "hdc", "-t", device.serial,
                "file", "send", *args,
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
            "hdc", "-t", device.serial, "shell",
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