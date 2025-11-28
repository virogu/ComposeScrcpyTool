/*
 * Copyright 2025 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.bean

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

//drwxr-xr-x  -rwxrwxr-x
//-：普通文件
//d：文件夹
//l：符号连接(软连接/快捷方式) 后面会用 -> 打印出指向的真实文件
enum class FileType(val sortIndex: Int) {
    DIR(0),
    LINK(0),
    FILE(1),
    OTHER(2)
}

sealed class RemoteFileLoadState() {
    object NotLoad : RemoteFileLoadState()
    object Loading : RemoteFileLoadState()
    object Loaded : RemoteFileLoadState()
    data class Error(val msg: String) : RemoteFileLoadState()
}

data class RemoteFile(
    val name: String = "",
    val parent: RemoteFile? = null,
    val path: String = "",
    val type: FileType = FileType.OTHER,
    val size: String = "",
    val modificationTime: String = "",
    val permissions: String = "",
    val level: Int = 0,
    val isExpanded: MutableState<Boolean> = mutableStateOf(false),
    val children: MutableState<List<RemoteFile>> = mutableStateOf(emptyList()),
    val childrenLoadStates: MutableState<RemoteFileLoadState> = mutableStateOf(RemoteFileLoadState.NotLoad)
) {
    var verifyInfo: FileVerifyInfo? = null

    val isDirectory = type == FileType.DIR

    val permission: FilePermission by lazy {
        FilePermission.parse(permissions) ?: FilePermission()
    }

    fun toggleExpand(targetExpanded: Boolean = !isExpanded.value) {
        if (isDirectory) {
            //关闭目录时把子目录都关闭
            if (!targetExpanded) {
                children.value.forEach {
                    it.toggleExpand(false)
                }
            }
            isExpanded.value = targetExpanded
        }
    }

    fun refresh() {
        if (isDirectory) {
            childrenLoadStates.value = RemoteFileLoadState.NotLoad
            children.value = emptyList()
        } else {
            parent?.refresh()
        }
    }

    companion object {
        val ROOT = RemoteFile(
            name = "",
            path = "",
            type = FileType.DIR,
            isExpanded = mutableStateOf(true)
        )
    }
}

data class FileVerifyInfo(
    val md5: Result<String>,
    val sha1: Result<String>,
)

private val Boolean.value: Int get() = if (this) 1 else 0

data class FilePermission(
    val owner: Normal = Normal(),
    val group: Normal = Normal(),
    val other: Normal = Normal(),
    val special: Special = Special()
) {
    //0777
    val value: String = "${special.s}${owner.s}${group.s}${other.s}"

    //rwxrwxrwx
    val desc: String = buildString {
        if (owner.readable) append("r") else append("-")
        if (owner.writeable) append("w") else append("-")
        if (special.setUid) {
            if (owner.executable) append("s") else append("S")
        } else {
            if (owner.executable) append("x") else append("-")
        }

        if (group.readable) append("r") else append("-")
        if (group.writeable) append("w") else append("-")
        if (special.setGid) {
            if (group.executable) append("s") else append("S")
        } else {
            if (group.executable) append("x") else append("-")
        }

        if (other.readable) append("r") else append("-")
        if (other.writeable) append("w") else append("-")
        if (special.sticky) {
            if (other.executable) append("t") else append("T")
        } else {
            if (other.executable) append("x") else append("-")
        }
    }

    data class Special(
        val setUid: Boolean = false,
        val setGid: Boolean = false,
        val sticky: Boolean = false,
    ) {
        constructor(value: Int) : this(
            value and 0b100 != 0,
            value and 0b010 != 0,
            value and 0b001 != 0,
        )

        val value = setUid.value * 4 + setGid.value * 2 + sticky.value * 1
        val s: String = value.toString()
    }

    data class Normal(
        val readable: Boolean = false,
        val writeable: Boolean = false,
        val executable: Boolean = false,
    ) {
        constructor(value: Int) : this(
            value and 0b100 != 0,
            value and 0b010 != 0,
            value and 0b001 != 0,
        )

        val value = readable.value * 4 + writeable.value * 2 + executable.value * 1
        val s: String = value.toString()
    }

    companion object {
        fun parse(permission: String): FilePermission? {
            return try {
                val p = permission.takeLast(9)
                FilePermission(
                    owner = Normal(
                        readable = p[0] == 'r',
                        writeable = p[1] == 'w',
                        executable = p[2] == 'x' || p[2] == 's',
                    ),
                    group = Normal(
                        readable = p[3] == 'r',
                        writeable = p[4] == 'w',
                        executable = p[5] == 'x' || p[2] == 's',
                    ),
                    other = Normal(
                        readable = p[6] == 'r',
                        writeable = p[7] == 'w',
                        executable = p[8] == 'x' || p[2] == 't',
                    ),
                    special = Special(
                        setUid = p[2].lowercaseChar() == 's',
                        setGid = p[5].lowercaseChar() == 's',
                        sticky = p[8].lowercaseChar() == 't',
                    )
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        fun parseIntString(s: String): FilePermission? {
            if (s.length != 4) {
                return null
            }
            val special = s[0].digitToIntOrNull() ?: return null
            val owner = s[1].digitToIntOrNull() ?: return null
            val group = s[2].digitToIntOrNull() ?: return null
            val other = s[3].digitToIntOrNull() ?: return null
            return parseIntString(special, owner, group, other)
        }

        fun parseIntString(special: Int, owner: Int, group: Int, other: Int): FilePermission? {
            if (special < 0 || special > 7) return null
            if (owner < 0 || owner > 7) return null
            if (group < 0 || group > 7) return null
            if (other < 0 || other > 7) return null
            return FilePermission(
                owner = Normal(owner),
                group = Normal(group),
                other = Normal(other),
                special = Special(special),
            )
        }
    }
}