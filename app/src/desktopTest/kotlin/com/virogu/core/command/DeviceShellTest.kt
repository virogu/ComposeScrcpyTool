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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceShellTest {

    @Test
    fun wrapWithExitMarker() {
        val wrapped = DeviceShell.wrapWithExitMarker("ls -l '/sdcard'")
        assertTrue(wrapped.endsWith("; echo; echo __RC:\$?"))
        assertTrue(wrapped.startsWith("ls -l '/sdcard'"))
    }

    @Test
    fun parseNormalOutput() {
        val sr = DeviceShell.parseShellResult("file1\nfile2\n\n__RC:0")
        assertEquals(0, sr.exitCode)
        assertEquals("file1\nfile2", sr.output)
    }

    @Test
    fun parseNonZeroExit() {
        val sr = DeviceShell.parseShellResult("ls: /data: Permission denied\n\n__RC:1")
        assertEquals(1, sr.exitCode)
        assertEquals("ls: /data: Permission denied", sr.output)
    }

    @Test
    fun parseCrlfOutput() {
        val sr = DeviceShell.parseShellResult("data line\r\n\r\n__RC:0\r\n")
        assertEquals(0, sr.exitCode)
        assertEquals("data line", sr.output)
    }

    @Test
    fun parseMissingMarker() {
        // su 被拒、连接中断等场景：没有标记时 exitCode = -1，输出保留原文
        val sr = DeviceShell.parseShellResult("su: permission denied")
        assertEquals(-1, sr.exitCode)
        assertEquals("su: permission denied", sr.output)
    }

    @Test
    fun parseLastMarkerWins() {
        // 输出中可能混入形似标记的行，必须取最后一个
        val sr = DeviceShell.parseShellResult("x\n__RC:9\ny\n\n__RC:0")
        assertEquals(0, sr.exitCode)
        assertEquals("x\n__RC:9\ny", sr.output)
    }

    @Test
    fun parseMarkerWithTrailingSpaces() {
        val sr = DeviceShell.parseShellResult("out\n\n__RC:2  \n")
        assertEquals(2, sr.exitCode)
        assertEquals("out", sr.output)
    }

    @Test
    fun escapeSingleQuotes() {
        assertEquals("it'\\''s", DeviceShell.escapeSingleQuotes("it's"))
        // 转义后的串嵌入单引号上下文时，原始的每个单引号都变成 '\'' 序列
        val original = "/sdcard/it's file"
        val embedded = "'" + DeviceShell.escapeSingleQuotes(original) + "'"
        assertEquals("'/sdcard/it'\\''s file'", embedded)
    }
}
