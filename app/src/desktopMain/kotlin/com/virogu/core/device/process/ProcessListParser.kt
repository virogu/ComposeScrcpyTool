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

package com.virogu.core.device.process

data class ParsedProcess(
    val user: String,
    val pid: String,
    val processName: String,
)

/**
 * `ps` / `ps -A` 输出解析（纯函数，可单测）。
 *
 * 两代已知格式：
 * - toolbox（Android 5.x ~ 6.0）：USER PID PPID VSIZE RSS WCHAN PC NAME（8 列）
 * - toybox（Android 7.0+）：USER PID PPID VSZ RSS WCHAN ADDR S NAME（9 列）
 *
 * 首列恒为 USER、次列恒为 PID、末列恒为 NAME（进程名不含空格），
 * 按位置取值、不依赖列数。表头行（PID 列非数字）与非包名风格的
 * 内核线程/原生守护进程（如 kworker、/system/bin/logd、system_server）被过滤，
 * 只保留应用进程——与旧 `am dump` 只列 ProcessRecord 的行为对齐。
 */
object ProcessListParser {

    // 点分 Java 包名，可带 `:子进程` 后缀（如 cn.ubiot.zj:daemon）
    private val packageNameRegex = Regex("""[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+(:[a-zA-Z0-9_]+)*""")
    private val whitespaceRegex = Regex("""\s+""")
    private val numberRegex = Regex("""\d+""")

    fun parseAppProcesses(lines: List<String>): List<ParsedProcess> = lines.mapNotNull { raw ->
        val line = raw.trim()
        if (line.isEmpty()) return@mapNotNull null
        val tokens = line.split(whitespaceRegex)
        if (tokens.size < 3) return@mapNotNull null
        val user = tokens[0]
        val pid = tokens[1]
        // 表头行（USER PID PPID ...）的 PID 列非数字
        if (!numberRegex.matches(pid)) return@mapNotNull null
        val name = tokens.last()
        if (!packageNameRegex.matches(name)) return@mapNotNull null
        ParsedProcess(user, pid, name)
    }
}
