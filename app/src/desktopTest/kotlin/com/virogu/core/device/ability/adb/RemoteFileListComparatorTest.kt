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
import com.virogu.core.bean.RemoteFile
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteFileListComparatorTest {

    private fun file(name: String, type: FileType, permissions: String) = RemoteFile(
        name = name,
        path = "/sdcard/$name",
        type = type,
        permissions = permissions,
    )

    /** 样本取自小米 HyperOS 真机 /sdcard：ls 原始输出目录与文件穿插，必须按类型分组重排 */
    @Test
    fun dirsFirstThenFiles() {
        val scrambled = listOf(
            file(".7934039a", FileType.FILE, "-rw-rw----"),
            file(".DataStorage", FileType.DIR, "drwxrws---"),
            file(".duid", FileType.FILE, "-rw-rw----"),
            file(".xlDownload", FileType.DIR, "drwxrws---"),
            file("14相机预设", FileType.DIR, "drwxrws---"),
            file("Android", FileType.DIR, "drwxrws---"),
            file(".AppData", FileType.DIR, "drwxrws---"),
            file(".csj", FileType.DIR, "drwxrws---"),
            file(".exmu-cfg1.data", FileType.FILE, "-rw-rw----"),
        )
        val sorted = scrambled.sortedWith(RemoteFileListComparator)
        assertEquals(
            listOf(
                // 目录（含点目录）按名称不区分大小写
                ".AppData", ".csj", ".DataStorage", ".xlDownload", "14相机预设", "Android",
                // 文件在后
                ".7934039a", ".duid", ".exmu-cfg1.data",
            ),
            sorted.map { it.name }
        )
    }

    /** 软链目录与目录同级（sortIndex 相同），软链文件与文件同级 */
    @Test
    fun linkTypesGroupWithTargets() {
        val scrambled = listOf(
            file("plain.txt", FileType.FILE, "-rw-rw----"),
            file("dirLink", FileType.LINK_DIR, "lrwxrwxrwx"),
            file("zzDir", FileType.DIR, "drwxr-xr-x"),
            file("fileLink", FileType.LINK_FILE, "lrwxrwxrwx"),
            file("aaDir", FileType.DIR, "drwxr-xr-x"),
        )
        val sorted = scrambled.sortedWith(RemoteFileListComparator)
        assertEquals(
            listOf("aaDir", "dirLink", "zzDir", "fileLink", "plain.txt"),
            sorted.map { it.name }
        )
    }
}
