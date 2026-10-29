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

package com.virogu.core.bean

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteFileTest {

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", RemoteFile.formatFileSize(0))
        assertEquals("0 B", RemoteFile.formatFileSize(-10))
        assertEquals("512 B", RemoteFile.formatFileSize(512))
        assertEquals("1 KB", RemoteFile.formatFileSize(1024))
        assertEquals("1.5 KB", RemoteFile.formatFileSize(1536))
        assertEquals("12.06 KB", RemoteFile.formatFileSize(12345))
        assertEquals("1 MB", RemoteFile.formatFileSize(1024 * 1024))
        assertEquals("1.5 MB", RemoteFile.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.24 TB", RemoteFile.formatFileSize(2463654321215L))
    }

    @Test
    fun testDisplaySizeForFileAndDir() {
        val file = RemoteFile(
            name = "test.txt",
            type = FileType.FILE,
            size = "12345B"
        )
        assertEquals("12.06 KB", file.displaySize)

        val largeFile = RemoteFile(
            name = "big.bin",
            type = FileType.FILE,
            size = "2463654321215B"
        )
        assertEquals("2.24 TB", largeFile.displaySize)

        val dir = RemoteFile(
            name = "folder",
            type = FileType.DIR,
            size = "4096B"
        )
        assertEquals("", dir.displaySize)

        val linkDir = RemoteFile(
            name = "link_folder",
            type = FileType.LINK_DIR,
            size = "4096B"
        )
        assertEquals("", linkDir.displaySize)

        val linkFile = RemoteFile(
            name = "link_file",
            type = FileType.LINK_FILE,
            size = "100B"
        )
        assertEquals("100 B", linkFile.displaySize)
    }
}
