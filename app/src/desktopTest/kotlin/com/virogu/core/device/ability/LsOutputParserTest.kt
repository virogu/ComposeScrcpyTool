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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LsOutputParserTest {

    /** toybox（Android 7.0+，含 nlink 列）：/sdcard 典型输出 */
    @Test
    fun parseToyboxFormat() {
        val lines = listOf(
            "drwxrwx--x 2 root sdcard_rw 4096 2026-01-02 03:04 Alarms",
            "drwxrwx--x 2 root sdcard_rw 4096 2026-01-02 03:04 Android",
            "-rw-rw---- 1 root sdcard_rw 12345 2026-02-03 04:05 我的 文件.txt",
            "lrwxrwxrwx 1 root root 21 2026-01-01 00:00 etc -> /system/etc",
            "crw-rw---- 1 root root 10, 61 2026-01-01 00:00 network_latency"
        )
        val outcome = LsOutputParser.parse(lines)
        assertTrue(outcome.errorLines.isEmpty())
        assertEquals(5, outcome.entries.size)

        val alarms = outcome.entries[0]
        assertEquals("Alarms", alarms.name)
        assertEquals(FileType.DIR, alarms.type)
        assertEquals("4096B", alarms.sizeText)
        assertEquals("2026-01-02 03:04", alarms.modificationTime)

        val chinese = outcome.entries[2]
        assertEquals("我的 文件.txt", chinese.name)
        assertEquals(FileType.FILE, chinese.type)
        assertEquals("12345B", chinese.sizeText)

        val etc = outcome.entries[3]
        assertEquals("etc", etc.name)
        assertEquals(FileType.LINK_FILE, etc.type)
        assertEquals("/system/etc", etc.linkTarget)

        // 设备文件：major, minor 被当作大小仅影响展示，类型必须是 OTHER
        val dev = outcome.entries[4]
        assertEquals("network_latency", dev.name)
        assertEquals(FileType.OTHER, dev.type)
        assertEquals("61B", dev.sizeText)
    }

    /** toolbox（Android 5.x ~ 6.0，无 nlink 列）：/sdcard 典型输出，含 -a 带出的 . 和 .. */
    @Test
    fun parseToolboxFormat() {
        val lines = listOf(
            "drwxrwx--x root     sdcard_r          4096 2026-01-02 03:04 Alarms",
            "-rw------- root     root           1234 2026-02-03 04:05 my file.txt",
            "crw------- root     root    10,   61 2026-01-01 00:00 network_latency",
            "drwxr-xr-x root     root             4096 2026-01-01 00:00 .",
            "drwxr-xr-x root     root             4096 2026-01-01 00:00 .."
        )
        val outcome = LsOutputParser.parse(lines)
        assertEquals(3, outcome.entries.size)
        assertEquals("Alarms", outcome.entries[0].name)
        assertEquals(FileType.DIR, outcome.entries[0].type)
        assertEquals("my file.txt", outcome.entries[1].name)
        assertEquals("1234B", outcome.entries[1].sizeText)
        assertEquals("network_latency", outcome.entries[2].name)
    }

    /** 文件名含空格、引号、$、反引号，都不影响解析 */
    @Test
    fun parseSpecialCharNames() {
        val lines = listOf(
            "-rw------- 1 root root 100 2026-01-01 00:00 it's a \$file `x`.txt",
            "-rw------- 1 root root 100 2026-01-01 00:00 name with  double space.txt"
        )
        val entries = LsOutputParser.parse(lines).entries
        assertEquals("it's a \$file `x`.txt", entries[0].name)
        assertEquals(FileType.FILE, entries[0].type)
        // 连续空格在按空白切分的输出里无法原样保留，这里验证文件名尾部 token 不丢
        assertTrue(entries[1].name.endsWith("double space.txt"))
    }

    /** 普通文件名里含 " -> " 不能被误判为软链接；软链接名里含 " -> " 时取最后一个分隔符 */
    @Test
    fun parseSymlinkArrow() {
        val lines = listOf(
            "-rw------- 1 root root 100 2026-01-01 00:00 a -> b.txt",
            "lrwxrwxrwx 1 root root 7 2026-01-01 00:00 a -> b -> /data/x"
        )
        val entries = LsOutputParser.parse(lines).entries
        assertEquals("a -> b.txt", entries[0].name)
        assertEquals(FileType.FILE, entries[0].type)
        assertEquals("", entries[0].linkTarget)

        assertEquals("a -> b", entries[1].name)
        assertEquals(FileType.LINK_FILE, entries[1].type)
        assertEquals("/data/x", entries[1].linkTarget)
    }

    /** 文件名本身长得像 ls 的日期时间列，不能把文件名误当日期锚点 */
    @Test
    fun parseDateLikeFileName() {
        val lines = listOf(
            "-rw------- 1 root root 100 2026-01-01 00:00 2026-01-01 12:00.txt",
            "-rw------- 1 root root 100 2026-01-01 00:00 2026-01-01 12:00"
        )
        val entries = LsOutputParser.parse(lines).entries
        assertEquals("2026-01-01 12:00.txt", entries[0].name)
        assertEquals("2026-01-01 00:00", entries[0].modificationTime)
        assertEquals("2026-01-01 12:00", entries[1].name)
    }

    /** CRLF 行尾（老 adbd 的 pty 会把 LF 转成 CRLF） */
    @Test
    fun parseCrlf() {
        val lines = listOf("drwxr-xr-x 1 root root 4096 2026-01-01 00:00 dir\r")
        val entries = LsOutputParser.parse(lines).entries
        assertEquals(listOf("dir"), entries.map { it.name })
    }

    /** 错误行（如 /sdcard/Android/data 无权限）被单独收集，不混进条目 */
    @Test
    fun parseErrorLines() {
        val outcome = LsOutputParser.parse(listOf("ls: /sdcard/Android/data: Permission denied"))
        assertTrue(outcome.entries.isEmpty())
        assertEquals(1, outcome.errorLines.size)
        assertTrue(outcome.errorLines[0].contains("Permission denied"))

        // 错误行与正常条目混合时，正常条目照常解析
        val mixed = LsOutputParser.parse(
            listOf(
                "ls: /x/link: Permission denied",
                "drwxr-xr-x 1 root root 4096 2026-01-01 00:00 ok"
            )
        )
        assertEquals(listOf("ok"), mixed.entries.map { it.name })
        assertEquals(1, mixed.errorLines.size)
    }

    /** busybox 风格（英文月份缩写）主解析解析不出来，走兜底 */
    @Test
    fun parseFallbackBusybox() {
        val lines = listOf(
            "drwxr-xr-x 2 root root 4096 Jan  1 2023 dir1",
            "-rw-r--r-- 1 root root 123 Jan 15 12:30 file2.txt"
        )
        assertTrue(LsOutputParser.parse(lines).entries.isEmpty())

        val entries = LsOutputParser.parseFallback(lines)
        assertEquals(2, entries.size)
        assertEquals("dir1", entries[0].name)
        assertEquals("4096B", entries[0].sizeText)
        assertEquals("Jan 1 2023", entries[0].modificationTime)
        assertEquals("file2.txt", entries[1].name)
        assertEquals("Jan 15 12:30", entries[1].modificationTime)
    }

    /** 兜底解析：中文本地化月份（`N月`） */
    @Test
    fun parseFallbackChineseMonth() {
        val lines = listOf("-rw-r--r-- 1 root root 123 1月 15 12:30 中文.txt")
        val entries = LsOutputParser.parseFallback(lines)
        assertEquals(1, entries.size)
        assertEquals("中文.txt", entries[0].name)
        assertEquals("1月 15 12:30", entries[0].modificationTime)
    }

    /** 空输出与干扰行 */
    @Test
    fun parseEmptyAndNoise() {
        assertTrue(LsOutputParser.parse(listOf("")).entries.isEmpty())
        assertTrue(LsOutputParser.parse(listOf("total 24")).entries.isEmpty())
    }
}
