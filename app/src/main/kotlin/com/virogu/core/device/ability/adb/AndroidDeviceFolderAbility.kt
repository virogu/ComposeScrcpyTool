package com.virogu.core.device.ability.adb

import com.virogu.core.bean.FileInfoItem
import com.virogu.core.bean.FileItem
import com.virogu.core.bean.FileTipsItem
import com.virogu.core.bean.FileType
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityFolder
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File
import java.util.regex.Pattern

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:46
 **/
class AndroidDeviceFolderAbility(device: Device) : DeviceAbilityFolder {
    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
    }

    private val target = arrayOf("-s", device.serial)

    override suspend fun remount(): String = buildString {
        cmd.adb(*target, "root").onSuccess {
            if (it.isNotEmpty()) {
                appendLine(it)
            }
        }.onFailure {
            it.printStackTrace()
            appendLine("restart with root fail")
        }
        cmd.adb(*target, "remount").onSuccess {
            if (it.isNotEmpty()) {
                appendLine(it)
            }
        }.onFailure {
            it.printStackTrace()
            appendLine("restart with root fail")
        }
    }

    override suspend fun refreshPath(path: String): Result<List<FileItem>> = cmd.adb(
        *target, "shell",
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

    override suspend fun createDir(dir: String, newFile: String): Result<String> = cmd.adb(
        *target, "shell", "mkdir '${dir}/${newFile}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }


    override suspend fun createFile(dir: String, newFile: String): Result<String> = cmd.adb(
        *target, "shell", "touch '${dir}/${newFile}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun deleteFile(fileItem: FileInfoItem): Result<String> = cmd.adb(
        *target, "shell", "rm -r '${fileItem.path}'",
        showLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun getFileDetail(fileItem: FileInfoItem): String = buildString {
        appendLine("路径: ${fileItem.path}")
        append("权限: ${fileItem.permissions}")
        append("  ")
        append("类型: ${fileItem.type.name}")
        append("  ")
        append("大小: ${fileItem.size}")
        appendLine()
        appendLine("修改时间: ${fileItem.modificationTime}")
        cmd.adb(
            *target,
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
        cmd.adb(
            *target,
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

    override suspend fun pullFile(fromFile: List<FileInfoItem>, toLocalFile: File): String = buildString {
        fromFile.forEach { f ->
            cmd.adb(
                *target,
                "pull", f.path, toLocalFile.absolutePath,
                showLog = true,
                timeout = 0L
            ).onSuccess {
                appendLine(it)
            }.onFailure {
                appendLine("pull file [${f.path}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun pushFile(toFile: FileInfoItem, fromLocalFiles: List<File>): String = buildString {
        fromLocalFiles.forEach { f ->
            val args = if (f.isDirectory) {
                arrayOf("${f.absolutePath}\\.", "${toFile.path}/${f.name}/.")
            } else {
                arrayOf(f.absolutePath, "${toFile.path}/${f.name}")
            }
            cmd.adb(
                *target,
                "push", *args,
                showLog = true
            ).onSuccess {
                appendLine(it)
            }.onFailure {
                appendLine("push file [${f.absolutePath}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun chmod(fileInfo: FileInfoItem, permission: String): String = buildString {
        cmd.adb(
            *target, "shell",
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