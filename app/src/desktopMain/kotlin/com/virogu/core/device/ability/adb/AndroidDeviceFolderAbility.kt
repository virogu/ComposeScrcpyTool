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
import com.virogu.core.command.DeviceShell
import com.virogu.core.command.ShellResult
import com.virogu.core.device.DeviceEntityAndroid
import com.virogu.core.device.ability.DeviceAbilityFolder
import com.virogu.core.device.ability.LsOutputParser
import com.virogu.core.device.ability.ParsedLsEntry
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File

/**
 * 文件列表统一排序：目录（含软链目录）在前，普通文件/软链文件居中，其他类型最后；
 * 同组内按名称不区分大小写。必须无条件执行——ls 的原始输出顺序跟随设备本地化排序，
 * 目录与文件会穿插，不能直接交给 UI。
 */
val RemoteFileListComparator: Comparator<RemoteFile> =
    compareBy({ it.realType.value.sortIndex }, { it.name.lowercase() })

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:46
 **/
class AndroidDeviceFolderAbility(device: DeviceEntityAndroid) : DeviceAbilityFolder {
    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
    }

    private val serial: String = device.serial
    private val target = arrayOf("-s", serial)

    /**
     * 单引号包裹的 shell 参数；路径中的 `'` 会被正确转义，
     * 配合 [execute] 里对 su 上下文的二次转义，文件名含空格/引号/`$` 都不会破坏命令。
     */
    private fun q(path: String): String = "'${DeviceShell.escapeSingleQuotes(path)}'"

    /**
     * 执行设备端命令并解析真实退出码：
     * - su 状态按设备画像只探测一次（[AdbDeviceProfileCache]），不再每条命令三连回退
     * - stderr 不再整体丢弃，失败原因可以透出到 UI，杜绝"输出为空即成功"的假成功
     */
    private suspend fun execute(
        commandStr: String,
        consoleLog: Boolean = true,
        timeout: Long = 30L
    ): Result<ShellResult> {
        val su = AdbDeviceProfileCache.suMode(serial)
        val body = when (su) {
            SuMode.NONE -> commandStr
            SuMode.SU0 -> "su 0 -c '${DeviceShell.escapeSingleQuotes(commandStr)}'"
            SuMode.SU_C -> "su -c '${DeviceShell.escapeSingleQuotes(commandStr)}'"
        }
        return cmd.adbChecked(
            *target,
            "shell",
            DeviceShell.wrapWithExitMarker(body),
            consoleLog = consoleLog,
            timeout = timeout
        ).mapCatching { sr ->
            val parsed = DeviceShell.parseShellResult(sr.output)
            if (parsed.exitCode < 0) {
                // 没拿到退出码标记：su 被拒、设备掉线等，必须报错而不是当空结果
                throw IllegalStateException(
                    parsed.output.ifEmpty { "设备无响应或命令未执行 (exit ${sr.exitCode})" }
                )
            }
            parsed
        }
    }

    /** 以设备端退出码为成败依据，失败时抛出设备原始报错文本 */
    private suspend fun execChecked(commandStr: String, consoleLog: Boolean = true, timeout: Long = 30L): Result<String> =
        execute(commandStr, consoleLog, timeout).mapCatching { sr ->
            if (sr.exitCode != 0) {
                throw IllegalStateException(sr.output.ifEmpty { "命令执行失败 (exit ${sr.exitCode})" })
            }
            sr.output.trim()
        }

    override suspend fun remount(): String = buildString {
        cmd.adbChecked(*target, "root", consoleLog = true).onSuccess {
            if (it.output.isNotEmpty()) {
                appendLine(it.output.trim())
            }
        }.onFailure {
            appendLine("restart with root fail, ${it.localizedMessage}")
        }
        cmd.adbChecked(*target, "remount", consoleLog = true).onSuccess {
            if (it.output.isNotEmpty()) {
                appendLine(it.output.trim())
            }
        }.onFailure {
            appendLine("restart with root fail, ${it.localizedMessage}")
        }
    }

    override suspend fun createDir(dir: String, newFile: String): Result<String> =
        execChecked("mkdir ${q("$dir/$newFile")}")

    override suspend fun createFile(dir: String, newFile: String): Result<String> =
        execChecked("touch ${q("$dir/$newFile")}")

    override suspend fun deleteFile(path: String): Result<String> =
        execChecked("rm -r ${q(path)}")

    override suspend fun getFileVerifyInfo(path: String): FileVerifyInfo = FileVerifyInfo(
        md5 = hashInfo("md5sum", path),
        sha1 = hashInfo("sha1sum", path)
    )

    private suspend fun hashInfo(cmdName: String, path: String): Result<String> =
        execChecked("$cmdName ${q(path)}", timeout = 0L).map { output ->
            val tokens = output.replace(Regex("\\s+"), " ").split(" ")
            if (tokens.size >= 2) tokens[0] else output
        }

    override suspend fun pullFile(toLocalFile: File, vararg fromRemotePath: String): String = buildString {
        fromRemotePath.forEach { path ->
            cmd.adbChecked(
                *target,
                "pull", path, toLocalFile.absolutePath,
                consoleLog = true,
                timeout = 0L
            ).onSuccess { sr ->
                if (sr.exitCode != 0) {
                    appendLine("pull file [${path}] fail, ${sr.output.ifEmpty { "exit ${sr.exitCode}" }}")
                } else {
                    appendLine(sr.output.ifEmpty { "pull $path success" })
                }
            }.onFailure {
                appendLine("pull file [${path}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun pushFile(toRemotePath: String, vararg fromLocalFiles: File): String = buildString {
        fromLocalFiles.forEach { f ->
            // 目录末尾补"分隔符+."表示推送目录内容；分隔符必须跟宿主平台
            // （旧实现写死 '\\'，Linux/Mac 下拼出的本地路径非法）
            val localArg = if (f.isDirectory) "${f.absolutePath}${File.separator}." else f.absolutePath
            val remoteArg = if (f.isDirectory) "${toRemotePath}/${f.name}/." else "${toRemotePath}/${f.name}"
            cmd.adbChecked(
                *target,
                "push", localArg, remoteArg,
                consoleLog = true,
                // 大文件推送不能吃 5 秒默认超时（超时会被当作取消而静默失败）
                timeout = 0L
            ).onSuccess { sr ->
                if (sr.exitCode != 0) {
                    appendLine("push file [${f.absolutePath}] fail, ${sr.output.ifEmpty { "exit ${sr.exitCode}" }}")
                } else {
                    appendLine(sr.output.ifEmpty { "push ${f.name} success" })
                }
            }.onFailure {
                appendLine("push file [${f.absolutePath}] fail, ${it.localizedMessage}")
            }
        }
    }

    override suspend fun chmod(path: String, permission: String): String = buildString {
        execChecked("chmod $permission ${q(path)}").onSuccess {
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
        // toolbox(Android 5.x~6.0) 不支持 -A/-h；toybox(Android 7.0+) 钉死绝对路径，
        // 避免 su 环境下 PATH 里的 busybox 等劫持 ls 导致输出格式漂移。
        // 回退链只有非末位分支允许吞 stderr，末位分支的报错必须可见。
        val cmdStr = "/system/bin/toybox ls -A -l ${q(targetPath)} 2>/dev/null" +
                " || ls -A -l ${q(targetPath)} 2>/dev/null" +
                " || ls -l -a ${q(targetPath)}"
        return execute(cmdStr, timeout = 30L).mapCatching { sr ->
            val lines = sr.output.lines()
            val outcome = LsOutputParser.parse(lines)
            var entries = outcome.entries
            if (entries.isEmpty() && outcome.errorLines.isEmpty()) {
                entries = LsOutputParser.parseFallback(lines)
            }
            if (entries.isEmpty()) {
                if (sr.exitCode != 0 || sr.output.isNotBlank()) {
                    // 真失败：透出设备原始报错（如 /sdcard/Android/data 的 Permission denied）
                    val reason = outcome.errorLines.joinToString("\n")
                        .ifEmpty { sr.output.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty() }
                        .ifEmpty { "命令执行失败 (exit ${sr.exitCode})" }
                    throw IllegalStateException(reason)
                }
                // 目录确实为空
                emptyList()
            } else {
                resolveSymlinkTypes(targetPath, toRemoteFiles(parent, entries))
            }
        }
    }

    /**
     * 批量判定软链接指向的是目录还是文件（`test -d` 跟随链接）。
     * 用序号回显代替回显路径本身，避免路径中的特殊字符污染输出解析。
     */
    private suspend fun resolveSymlinkTypes(basePath: String, files: List<RemoteFile>): List<RemoteFile> {
        val symlinkFiles = files.filter { it.permissions.startsWith("l", true) }
        val dirPaths = HashSet<String>()
        if (symlinkFiles.isNotEmpty()) {
            // 同级相对目标（如 acpi -> toybox）不含 '/'，在内核层不可能是目录，直接按 LINK_FILE 处理
            val targetsToTest = symlinkFiles.filter { it.linkTarget.contains("/") }
            if (targetsToTest.isNotEmpty()) {
                val batch = targetsToTest.mapIndexed { index, file ->
                    "test -d ${q(file.path)} && echo 'D$index'"
                }.joinToString("; ")
                // 与列表同通道（含 su 包装），保证 root 下对受限路径的判定一致；
                // su 状态在首次列表时已探测完成，这里不会再次触发授权弹窗
                execute(batch, consoleLog = false).onSuccess { sr ->
                    sr.output.lineSequence().forEach { line ->
                        Regex("^D(\\d+)$").find(line.trim())?.let { m ->
                            targetsToTest.getOrNull(m.groupValues[1].toInt())?.let { dirPaths.add(it.path) }
                        }
                    }
                }
            }
        }

        // 排序必须无条件执行（目录里没有软链接时也要排），不能放在上面的分支里
        return files.map { file ->
            if (file.permissions.startsWith("l", true)) {
                val resolved = if (file.path in dirPaths) FileType.LINK_DIR else FileType.LINK_FILE
                file.realType.value = resolved
                file.copy(type = resolved)
            } else {
                file
            }
        }.sortedWith(RemoteFileListComparator)
    }

    private fun toRemoteFiles(parent: RemoteFile, entries: List<ParsedLsEntry>): List<RemoteFile> =
        entries.map {
            RemoteFile(
                it.name, parent, "${parent.path}/${it.name}",
                it.type, it.sizeText, it.modificationTime, it.permissions,
                level = parent.level + 1,
                linkTarget = it.linkTarget
            )
        }
}
