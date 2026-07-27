/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.device.ability.adb

import com.virogu.core.bean.FileType
import com.virogu.core.bean.FileVerifyInfo
import com.virogu.core.bean.RemoteFile
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.DeviceEntityAndroid
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
class AndroidDeviceFolderAbility(device: DeviceEntityAndroid) : DeviceAbilityFolder {
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

    private suspend fun execute(commandStr: String, consoleLog: Boolean = true): Result<String> {
        // 使用单行命令 `su 0 sh -c "cmd" 2>/dev/null || cmd`，确保在一次网络通信中完成提权尝试。
        // 如果 su 成功执行并返回 0，短路运算结束，非常快。
        // 如果 su 不存在或由于无权限等原因导致失败，标准错误被抑制，马上无缝执行正常的 cmd，并输出正常的报错信息。
        val escapedCmd = commandStr.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`")
        val combinedCmd = "su 0 sh -c \"$escapedCmd\" 2>/dev/null || $commandStr"
        return cmd.adb(*target, "shell", combinedCmd, consoleLog = consoleLog)
    }

    override suspend fun createDir(dir: String, newFile: String): Result<String> = execute(
        "mkdir '${dir}/${newFile}'",
        consoleLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }


    override suspend fun createFile(dir: String, newFile: String): Result<String> = execute(
        "touch '${dir}/${newFile}'",
        consoleLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun deleteFile(path: String): Result<String> = execute(
        "rm -r '${path}'",
        consoleLog = true
    ).mapCatching {
        if (it.isNotEmpty()) {
            throw IllegalStateException(it)
        } else {
            ""
        }
    }

    override suspend fun getFileVerifyInfo(path: String): FileVerifyInfo {
        val md5 = execute("md5sum '${path}'").map {
            it.replace("\\s+".toRegex(), " ").split(" ").let { l ->
                if (l.size == 2) {
                    l[0]
                } else {
                    it
                }
            }
        }
        val sha1 = execute("sha1sum '${path}'").map {
            it.replace("\\s+".toRegex(), " ").split(" ").let { l ->
                if (l.size == 2) {
                    l[0]
                } else {
                    it
                }
            }
        }
        return FileVerifyInfo(md5 = md5, sha1 = sha1)
    }

    override suspend fun pullFile(toLocalFile: File, vararg fromRemotePath: String): String = buildString {
        fromRemotePath.forEach { path ->
            cmd.adb(
                *target,
                "pull", path, toLocalFile.absolutePath,
                consoleLog = true,
                timeout = 0L
            ).onSuccess {
                appendLine(it)
            }.onFailure {
                appendLine("pull file [${path}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun pushFile(toRemotePath: String, vararg fromLocalFiles: File): String = buildString {
        fromLocalFiles.forEach { f ->
            val args = if (f.isDirectory) {
                arrayOf("${f.absolutePath}\\.", "${toRemotePath}/${f.name}/.")
            } else {
                arrayOf(f.absolutePath, "${toRemotePath}/${f.name}")
            }
            cmd.adb(
                *target,
                "push", *args,
                consoleLog = true
            ).onSuccess {
                appendLine(it)
            }.onFailure {
                appendLine("push file [${f.absolutePath}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun chmod(path: String, permission: String): String = buildString {
        execute("chmod $permission '${path}'", consoleLog = true).onSuccess {
            if (it.isNotBlank()) {
                appendLine(it)
            } else {
                appendLine("chmod $permission $path success")
            }
        }.onFailure {
            appendLine("chmod $permission $path fail, ${it.localizedMessage}")
        }
    }

    override suspend fun refreshPath(parent: RemoteFile, path: String): Result<List<RemoteFile>> =
        execute("ls -h -g -l -L -A '${path.ifEmpty { "/" }}'").map {
            val lines = it.trim().split("\n")
            val files: List<RemoteFile> = if (lines.isEmpty()) {
                emptyList()
            } else if (lines.size == 1 && Pattern.compile(".*\\$path.*Permission denied.*").matcher(lines.first())
                    .find()
            ) {
                //println("^(.*)?${parent}(.*)?Permission denied(.*)?$ find")
                throw IllegalStateException(lines.first())
            } else {
                lines.filterNot { l ->
                    Pattern.compile(".*\\$path.*Permission denied.*").matcher(l).find()
                }.parseToFiles(parent)
            }
            files
        }

    //> adb shell ls -h -g -lL /sdcard
    //total 91K
    //drwxrwx---  4 everybody 3.3K 2025-09-08 21:33 1Backup
    //drwxrwx---  5 everybody 3.3K 2025-06-29 13:33 Android
    //drwxrwx--- 18 everybody 3.3K 2025-09-24 10:35 DCIM
    //drwxrwx---  4 everybody 3.3K 2025-09-08 21:33 DataBackup
    //drwxrwx---  6 everybody 3.3K 2025-11-10 20:58 Download
    //drwxrwx---  9 everybody 3.3K 2025-10-11 14:04 MIUI
    //drwxrwx---  5 everybody 3.3K 2025-11-14 00:12 Movies
    //drwxrwx---  4 everybody  56K 2025-10-07 19:10 Music
    //drwxrwx--- 18 everybody 8.0K 2025-11-27 18:33 Pictures
    //drwxrwx---  3 everybody 3.3K 2025-11-10 21:02 com.milink.service
    //drwxrwx---  3 everybody 3.3K 2025-10-25 03:01 com.miui.voiceassist
    private fun List<String>.parseToFilesOld(parent: RemoteFile): List<RemoteFile> {
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
                RemoteFile(
                    name, parent, "${parent.path}/${name}",
                    type, size, modificationTime, permissions,
                    level = parent.level + 1
                )
            }
            return files.sortedBy {
                it.type.sortIndex
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun List<String>.parseToFiles(parent: RemoteFile): List<RemoteFile> {
        if (this.isEmpty()) {
            return emptyList()
        }
        try {
            val files = this.mapNotNull { line ->
                val trimLine = line.trim()
                if (trimLine.isEmpty()) return@mapNotNull null

                val tokens = trimLine.split("\\s+".toRegex())
                if (tokens.size < 6) return@mapNotNull null

                val permissions = tokens[0]
                if (permissions.startsWith("l", ignoreCase = true)) {
                    return@mapNotNull null
                }

                var dateStartIndex = -1
                for (i in 3 until tokens.size - 1) {
                    val token = tokens[i]
                    if (token.matches(Regex("\\d{4}[-/]\\d{2}[-/]\\d{2}")) ||
                        token.matches(Regex("[a-zA-Z]{3}")) ||
                        token.matches(Regex("\\d{1,2}月"))
                    ) {
                        dateStartIndex = i
                        break
                    }
                }

                if (dateStartIndex == -1) {
                    return@mapNotNull listOf(line).parseToFilesOld(parent).firstOrNull()
                }

                val sizeStr = tokens[dateStartIndex - 1]
                val size = if (sizeStr.firstOrNull()?.isDigit() == true) sizeStr.plus("B") else "0B"

                val isThreePartDate = tokens[dateStartIndex].length < 8
                val nameStartIndex = if (isThreePartDate) dateStartIndex + 3 else dateStartIndex + 2

                if (nameStartIndex > tokens.size) {
                    return@mapNotNull listOf(line).parseToFilesOld(parent).firstOrNull()
                }

                val modificationTime = tokens.subList(dateStartIndex, nameStartIndex).joinToString(" ")
                val name = tokens.subList(nameStartIndex, tokens.size).joinToString(" ")

                if (name.isEmpty()) return@mapNotNull null

                val type = when {
                    permissions.startsWith("-") -> FileType.FILE
                    permissions.startsWith("d", true) -> FileType.DIR
                    else -> FileType.OTHER
                }

                RemoteFile(
                    name, parent, "${parent.path}/$name",
                    type, size, modificationTime, permissions,
                    level = parent.level + 1
                )
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