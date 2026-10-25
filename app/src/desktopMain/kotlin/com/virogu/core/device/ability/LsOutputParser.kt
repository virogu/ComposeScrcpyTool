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

package com.virogu.core.device.ability

import com.virogu.core.bean.FileType

data class ParsedLsEntry(
    val name: String,
    val type: FileType,
    val sizeText: String,
    val modificationTime: String,
    val permissions: String,
    val linkTarget: String,
)

/**
 * `ls -l` 输出解析器（纯函数，不依赖设备/DI，可单测）。
 *
 * 两套已知的稳定输出格式：
 * - toolbox（Android 5.x ~ 6.0）：mode user group size date name，无 nlink 列
 * - toybox（Android 7.0+）：mode nlink owner group size date name
 *
 * 两家的日期都是硬编码 `%Y-%m-%d %H:%M`（不随系统语言变化），因此主解析以
 * "日期+时间相邻对"为锚点定位文件名，不按固定列下标取值，nlink 有无、设备文件的
 * `major, minor` 都能兼容。文件名 = 日期之后的全部 token（空格天然安全）。
 */
object LsOutputParser {

    private val dateRegex = Regex("""\d{4}[-/]\d{2}[-/]\d{2}""")
    private val timeRegex = Regex("""\d{1,2}:\d{2}(:\d{2})?""")
    private val numberRegex = Regex("""\d+""")
    private val whitespaceRegex = Regex("""\s+""")
    private val symlinkArrow = " -> "

    data class Outcome(
        val entries: List<ParsedLsEntry>,
        val errorLines: List<String>,
    )

    fun parse(lines: List<String>): Outcome {
        val entries = ArrayList<ParsedLsEntry>()
        val errors = ArrayList<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (isErrorLine(line)) {
                errors.add(line)
                continue
            }
            parseEntry(line)?.let { entries.add(it) }
        }
        return Outcome(entries, errors)
    }

    fun isErrorLine(line: String): Boolean = line.startsWith("ls:", ignoreCase = true) ||
            line.contains("Permission denied", ignoreCase = true) ||
            line.contains("No such file", ignoreCase = true) ||
            line.contains("Not a directory", ignoreCase = true)

    private fun parseEntry(line: String): ParsedLsEntry? {
        val tokens = line.split(whitespaceRegex)
        if (tokens.size < 5) return null
        val permissions = tokens[0]

        var dateIdx = -1
        for (i in 3 until tokens.size - 1) {
            if (dateRegex.matches(tokens[i]) && timeRegex.matches(tokens[i + 1])) {
                dateIdx = i
                break
            }
        }
        if (dateIdx < 2) return null

        val rawName = tokens.subList(dateIdx + 2, tokens.size).joinToString(" ")
        var name = rawName
        var target = ""
        if (permissions.startsWith("l", ignoreCase = true)) {
            // 只有软链接按 `name -> target` 切分；文件名自身含 " -> " 时取最后一个分隔符
            val idx = rawName.lastIndexOf(symlinkArrow)
            if (idx >= 0) {
                name = rawName.substring(0, idx).trim()
                target = rawName.substring(idx + symlinkArrow.length).trim()
            }
        }
        if (name.isEmpty() || name == "." || name == "..") return null

        // 大小取日期前最后一个纯数字 token（toolbox 设备文件为 `major, minor`，取到的是 minor，仅影响展示）
        var sizeText = "0B"
        for (j in dateIdx - 1 downTo 1) {
            if (numberRegex.matches(tokens[j])) {
                sizeText = "${tokens[j]}B"
                break
            }
        }

        val type = when {
            permissions.startsWith("-") -> FileType.FILE
            permissions.startsWith("d", true) -> FileType.DIR
            permissions.startsWith("l", true) -> FileType.LINK_FILE
            else -> FileType.OTHER
        }
        return ParsedLsEntry(
            name = name,
            type = type,
            sizeText = sizeText,
            modificationTime = "${tokens[dateIdx]} ${tokens[dateIdx + 1]}",
            permissions = permissions,
            linkTarget = target,
        )
    }

    /**
     * 兜底解析：被 busybox 等非标准 ls 劫持 PATH 时的启发式（英文月份缩写 / 中文 `N月`）。
     * 输出格式不受我们控制，只保证尽量提取条目，不作为主路径。
     */
    fun parseFallback(lines: List<String>): List<ParsedLsEntry> {
        val validMonths = setOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        val entries = ArrayList<ParsedLsEntry>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || isErrorLine(line)) continue
            val tokens = line.split(whitespaceRegex)
            if (tokens.size < 6) continue
            val permissions = tokens[0]

            var dateStartIndex = -1
            for (i in 3 until tokens.size - 1) {
                val token = tokens[i]
                if (dateRegex.matches(token) ||
                    validMonths.contains(token.lowercase()) ||
                    token.matches(Regex("""\d{1,2}月"""))
                ) {
                    dateStartIndex = i
                    break
                }
            }
            if (dateStartIndex == -1) continue

            val sizeStr = tokens[dateStartIndex - 1]
            val sizeText = if (sizeStr.firstOrNull()?.isDigit() == true) "${sizeStr}B" else "0B"
            val isThreePartDate = tokens[dateStartIndex].length < 8
            val nameStartIndex = if (isThreePartDate) dateStartIndex + 3 else dateStartIndex + 2
            if (nameStartIndex > tokens.size) continue

            val modificationTime = tokens.subList(dateStartIndex, nameStartIndex).joinToString(" ")
            val rawName = tokens.subList(nameStartIndex, tokens.size).joinToString(" ")
            var name = rawName
            var target = ""
            if (permissions.startsWith("l", ignoreCase = true)) {
                val idx = rawName.lastIndexOf(symlinkArrow)
                if (idx >= 0) {
                    name = rawName.substring(0, idx).trim()
                    target = rawName.substring(idx + symlinkArrow.length).trim()
                }
            }
            if (name.isEmpty() || name == "." || name == "..") continue

            val type = when {
                permissions.startsWith("-") -> FileType.FILE
                permissions.startsWith("d", true) -> FileType.DIR
                permissions.startsWith("l", true) -> FileType.LINK_FILE
                else -> FileType.OTHER
            }
            entries.add(ParsedLsEntry(name, type, sizeText, modificationTime, permissions, target))
        }
        return entries
    }
}
