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

package com.virogu.core.command

/**
 * 设备端 shell 的统一约定（adb/hdc 共用）：
 *
 * Android 5/6 的 adbd 没有 shell 协议，`adb shell` 永远向宿主返回退出码 0，
 * 因此设备命令的真实退出码统一靠在命令尾部追加 `; echo; echo __RC:$?` 标记获取
 * （由设备端 mksh 求值，全版本可用）。前一个 echo 保证标记永远独立成行，
 * 即使上一条命令的输出没有换行结尾也不会粘连。
 *
 * [escapeSingleQuotes] 用于把命令字符串嵌入 `su -c '...'` 的单引号上下文。
 */
object DeviceShell {

    const val RC_MARKER = "__RC:"

    private val rcRegex = Regex("^${Regex.escape(RC_MARKER)}(-?\\d+)[ \\t]*$", RegexOption.MULTILINE)

    fun escapeSingleQuotes(command: String): String = command.replace("'", "'\\''")

    fun wrapWithExitMarker(command: String): String = "$command; echo; echo $RC_MARKER\$?"

    /**
     * 从设备输出解析退出码标记，返回 (exitCode, 业务输出)。
     * 输出中可能混有内容相似的行，取最后一个匹配；找不到标记时 exitCode 为 -1
     * （如 su 被拒、连接中断），此时输出为剥离 \r 后的原始输出。
     */
    fun parseShellResult(raw: String): ShellResult {
        val clean = raw.replace("\r\n", "\n").replace('\r', '\n')
        val matches = rcRegex.findAll(clean).toList()
        if (matches.isEmpty()) {
            return ShellResult(-1, clean.trim())
        }
        val last = matches.last()
        val code = last.groupValues[1].toInt()
        val output = clean.removeRange(last.range).trim()
        return ShellResult(code, output)
    }
}
