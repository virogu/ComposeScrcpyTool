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

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessListParserTest {

    /** Android 5.0.2 模拟器（toolbox ps，8 列）真实输出节选 */
    @Test
    fun parseToolboxPs() {
        val lines = listOf(
            "USER     PID   PPID  VSIZE  RSS     WCHAN    PC        NAME",
            "root      1     0     2944   624   810f9686 0042403a S /init",
            "system    1545  1182  1402056 95848 ffffffff 676a0a8a S system_server",
            "u0_a12    1671  1182  1325484 79280 ffffffff 676a0a8a S com.android.systemui",
            "u0_a54    2344  1182  1415088 65312 ffffffff 676a0a8a S cn.ubiot.zj:daemon",
            "system    2553  1182  1277428 41760 ffffffff 676a0a8a S cn.ubiot.zj.install",
            "u0_a54    3541  1182  1584508 202768 ffffffff 676a0a8a S cn.ubiot.zj",
            "root      3752  2     0      0     81044800 00000000 S kworker/u8:1",
            "media_rw  1817  1     7908   1084  ffffffff db2cfee7 S /system/bin/sdcard"
        )
        val apps = ProcessListParser.parseAppProcesses(lines)
        // 只保留包名风格的应用进程：内核线程、system_server、/system 原生守护进程全部排除
        assertEquals(
            listOf(
                "com.android.systemui" to "1671",
                "cn.ubiot.zj:daemon" to "2344",
                "cn.ubiot.zj.install" to "2553",
                "cn.ubiot.zj" to "3541",
            ),
            apps.map { it.processName to it.pid }
        )
        // user 列原样保留（应用进程可能是 u0_aXX，也可能是 system 等系统 uid）
        assertEquals("system", apps.first { it.processName == "cn.ubiot.zj.install" }.user)
    }

    /** Android 7+（toybox ps -A，9 列）格式：ADDR、S 列多一列，解析规则不变 */
    @Test
    fun parseToyboxPs() {
        val lines = listOf(
            "USER            PID   PPID     VSZ    RSS WCHAN            ADDR S NAME",
            "root                 1      0   12284   4124 do_exit          0 S init",
            "u0_a123            5678   1234 4251648 128512 0                   0 S com.example.app",
            "u0_a123            5679   1234 4251648 128512 0                   0 S com.example.app:push",
            "root                999      2      0      0 0                   0 S [kthreadd]"
        )
        val apps = ProcessListParser.parseAppProcesses(lines)
        assertEquals(
            listOf("com.example.app" to "5678", "com.example.app:push" to "5679"),
            apps.map { it.processName to it.pid }
        )
    }

    /** 表头、空行、残缺行跳过；子进程包名截取冒号前为主包名 */
    @Test
    fun parseNoiseAndPackageName() {
        val lines = listOf(
            "",
            "USER PID PPID VSZ RSS WCHAN ADDR S NAME",
            "u0_a1 100 1 100 100 0 0 S com.a.b",
            "root 1"
        )
        val apps = ProcessListParser.parseAppProcesses(lines)
        assertEquals(listOf("com.a.b"), apps.map { it.processName })
        assertEquals("com.a.b", apps[0].processName.substringBefore(':'))
    }
}
