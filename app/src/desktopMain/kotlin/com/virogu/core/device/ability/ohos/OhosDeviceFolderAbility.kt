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

package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.FileType
import com.virogu.core.bean.FileVerifyInfo
import com.virogu.core.bean.RemoteFile
import com.virogu.core.command.DeviceShell
import com.virogu.core.command.HdcCommand
import com.virogu.core.command.ShellResult
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
class OhosDeviceFolderAbility(device: Device) : DeviceAbilityFolder {
    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
        private const val DEBUG = false
    }

    private val target = arrayOf("-t", device.serial)

    override suspend fun remount(): String = buildString {
        cmd.hdc(*target, "shell", "mount -o rw,remount /", consoleLog = DEBUG).onSuccess {
            if (it.isNotEmpty()) {
                appendLine(it)
            } else {
                appendLine("mount / success")
            }
        }.onFailure {
            it.printStackTrace()
            appendLine("remount fail")
        }
    }

    private suspend fun executeShellWithFallback(commandStr: String, consoleLog: Boolean = true): Result<ShellResult> {
        val escapedCmd = DeviceShell.escapeSingleQuotes(commandStr)
        val combinedCmd = "su 0 -c '$escapedCmd' 2>/dev/null || su -c '$escapedCmd' 2>/dev/null || $commandStr"
        // hdc shell 不回传设备端退出码，统一用标记法获取真实结果，
        // 避免"输出为空即成功"把权限不足等失败吞成假成功
        return cmd.hdcChecked(*target, "shell", DeviceShell.wrapWithExitMarker(combinedCmd), consoleLog = consoleLog)
            .mapCatching { sr ->
                val parsed = DeviceShell.parseShellResult(sr.output)
                if (parsed.exitCode < 0) {
                    throw IllegalStateException(parsed.output.ifEmpty { "设备无响应或命令未执行 (exit ${sr.exitCode})" })
                }
                parsed
            }
    }

    override suspend fun refreshPath(
        parent: RemoteFile, path: String
    ): Result<List<RemoteFile>> = executeShellWithFallback(
        "ls -l -A -h '${path.ifEmpty { "/" }}'", consoleLog = DEBUG
    ).mapCatching { sr ->
        if (sr.exitCode != 0) {
            throw IllegalStateException(
                sr.output.lineSequence().firstOrNull { it.isNotBlank() }
                    ?: "ls 失败 (exit ${sr.exitCode})"
            )
        }
        val lines = sr.output.trim().split("\n")
        lines.parseToFiles(parent)
    }

    override suspend fun createDir(dir: String, newFile: String): Result<String> = executeShellWithFallback(
        "mkdir -p '${dir}/${newFile}'",
        consoleLog = true
    ).mapCatching {
        if (it.exitCode != 0) {
            throw IllegalStateException(it.output.ifEmpty { "mkdir 失败 (exit ${it.exitCode})" })
        }
        it.output.trim()
    }


    override suspend fun createFile(dir: String, newFile: String): Result<String> = executeShellWithFallback(
        "touch '${dir}/${newFile}'",
        consoleLog = true
    ).mapCatching {
        if (it.exitCode != 0) {
            throw IllegalStateException(it.output.ifEmpty { "touch 失败 (exit ${it.exitCode})" })
        }
        it.output.trim()
    }

    override suspend fun deleteFile(path: String): Result<String> = executeShellWithFallback(
        "rm -r '${path}'",
        consoleLog = true
    ).mapCatching {
        if (it.exitCode != 0) {
            throw IllegalStateException(it.output.ifEmpty { "rm 失败 (exit ${it.exitCode})" })
        }
        it.output.trim()
    }

    override suspend fun getFileVerifyInfo(path: String): FileVerifyInfo = FileVerifyInfo(
        md5 = hashInfo("md5sum", path),
        sha1 = hashInfo("sha1sum", path)
    )

    private suspend fun hashInfo(cmdName: String, path: String): Result<String> =
        executeShellWithFallback("$cmdName '${path}'", consoleLog = DEBUG).mapCatching {
            if (it.exitCode != 0) {
                throw IllegalStateException(it.output.ifEmpty { "执行失败 (exit ${it.exitCode})" })
            }
            val collapsed = it.output.trim().replace(Regex("\\s+"), " ")
            val tokens = collapsed.split(" ")
            if (tokens.size >= 2) tokens[0] else collapsed
        }

    override suspend fun pullFile(toLocalFile: File, vararg fromRemotePath: String): String = buildString {
        fromRemotePath.forEach { path ->
            cmd.hdc(
                *target,
                "file", "recv", "-a", "\"${path}\"", "\"${toLocalFile.absolutePath}\"",
                consoleLog = true
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
                arrayOf("\"${f.absolutePath}\\.\"", "\"${toRemotePath}/${f.name}/.\"")
            } else {
                arrayOf("\"${f.absolutePath}\"", "\"${toRemotePath}/${f.name}\"")
            }
            cmd.hdc(
                *target,
                "file", "send", *args,
                consoleLog = true
            ).onSuccess {
                appendLine(it)
            }.onFailure {
                appendLine("push file [${f.absolutePath}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun chmod(path: String, permission: String): String = buildString {
        executeShellWithFallback("chmod $permission '${path}'", consoleLog = true).onSuccess { sr ->
            if (sr.exitCode != 0) {
                appendLine(sr.output.ifEmpty { "chmod fail (exit ${sr.exitCode})" })
            } else if (sr.output.isNotBlank()) {
                appendLine(sr.output.trim())
            } else {
                appendLine("chmod $permission $path success")
            }
        }.onFailure {
            appendLine("chmod $permission $path fail, ${it.localizedMessage}")
        }
    }

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
                    permissions.startsWith("l", true) -> FileType.LINK_FILE
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