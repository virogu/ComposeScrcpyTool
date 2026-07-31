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
        val escapedCmd = commandStr.replace("'", "'\\''")
        val combinedCmd = "su 0 -c '$escapedCmd' 2>/dev/null || su -c '$escapedCmd' 2>/dev/null || $commandStr"
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

    override suspend fun refreshPath(parent: RemoteFile, path: String): Result<List<RemoteFile>> {
        val targetPath = if (path.isEmpty() || path == "/") "/" else "${path.removeSuffix("/")}/"
        val cmdStr =
            "ls -l -A -h '$targetPath' 2>/dev/null || ls -l -a -h '$targetPath' 2>/dev/null || ls -l -a '$targetPath'"
        return execute(cmdStr).map {
            val lines = it.trim().split("\n")
            val firstLine = lines.firstOrNull().orEmpty()
            val isErrorLine = lines.size == 1 && (
                    firstLine.contains("Not a directory", ignoreCase = true) ||
                            firstLine.contains("No such file", ignoreCase = true) ||
                            firstLine.contains("Permission denied", ignoreCase = true)
                    )
            val files: List<RemoteFile> = if (lines.isEmpty()) {
                emptyList()
            } else if (isErrorLine) {
                throw IllegalStateException(firstLine)
            } else {
                lines.filterNot { l ->
                    l.contains("Permission denied", ignoreCase = true) ||
                            l.contains("Not a directory", ignoreCase = true) ||
                            l.contains("No such file", ignoreCase = true)
                }.parseToFiles(parent)
            }
            resolveSymlinkTypesBatch(targetPath, files)
        }
    }

    private suspend fun resolveSymlinkTypesBatch(basePath: String, files: List<RemoteFile>): List<RemoteFile> {
        val symlinkFiles = files.filter { it.permissions.startsWith("l", true) }
        if (symlinkFiles.isEmpty()) return files

        // 1. 同级相对路径（如 acpi -> toybox）不含 '/'，在内核层不可能为目录，瞬间 0ms 归类为 LINK_FILE
        val targetsToTest = symlinkFiles.filter { file ->
            val target = file.linkTarget
            target.contains("/")
        }

        val dirPaths = mutableSetOf<String>()
        if (targetsToTest.isNotEmpty()) {
            val batchCmd = targetsToTest.joinToString("; ") { file ->
                "test -d '${file.path}' && echo 'DIR:${file.path}'"
            }
            execute(batchCmd).onSuccess { out ->
                out.trim().split("\n").forEach { line ->
                    val trimLine = line.trim()
                    if (trimLine.startsWith("DIR:")) {
                        dirPaths.add(trimLine.substringAfter("DIR:"))
                    }
                }
            }
        }

        return files.map { file ->
            if (file.permissions.startsWith("l", true)) {
                val isDir = dirPaths.contains(file.path)
                val resolvedType = if (isDir) FileType.LINK_DIR else FileType.LINK_FILE
                file.realType.value = resolvedType
                file.copy(type = resolvedType)
            } else {
                file
            }
        }.sortedWith(compareBy({ it.realType.value.sortIndex }, { it.name.lowercase() }))
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

                var dateStartIndex = -1
                val validMonths =
                    setOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                for (i in 3 until tokens.size - 1) {
                    val token = tokens[i]
                    val lowerToken = token.lowercase()
                    if (token.matches(Regex("\\d{4}[-/]\\d{2}[-/]\\d{2}")) ||
                        validMonths.contains(lowerToken) ||
                        token.matches(Regex("\\d{1,2}月"))
                    ) {
                        dateStartIndex = i
                        break
                    }
                }

                if (dateStartIndex == -1) {
                    return@mapNotNull null
                }

                val sizeStr = tokens[dateStartIndex - 1]
                val size = if (sizeStr.firstOrNull()?.isDigit() == true) sizeStr.plus("B") else "0B"

                val isThreePartDate = tokens[dateStartIndex].length < 8
                val nameStartIndex = if (isThreePartDate) dateStartIndex + 3 else dateStartIndex + 2

                if (nameStartIndex > tokens.size) {
                    return@mapNotNull null
                }

                val modificationTime = tokens.subList(dateStartIndex, nameStartIndex).joinToString(" ")
                val rawName = tokens.subList(nameStartIndex, tokens.size).joinToString(" ")
                val parts = rawName.split(" -> ")
                val name = parts[0].trim()
                val target = parts.getOrNull(1)?.trim().orEmpty()

                if (name.isEmpty() || name == "." || name == "..") return@mapNotNull null

                val type = when {
                    permissions.startsWith("-") -> FileType.FILE
                    permissions.startsWith("d", true) -> FileType.DIR
                    permissions.startsWith("l", true) -> FileType.LINK_FILE
                    else -> FileType.OTHER
                }

                RemoteFile(
                    name, parent, "${parent.path}/$name",
                    type, size, modificationTime, permissions,
                    level = parent.level + 1,
                    linkTarget = target
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