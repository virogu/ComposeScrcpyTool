/*
 * Copyright 2024 Virogu
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

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Virogu
 * @since 2022-08-31 16:02
 **/

class LogEntity(val msg: String, val color: Color = Color.Unspecified)

fun SnapshotStateList<LogEntity>.plus(msg: String, color: Color = Color.Unspecified) {
    val newLog = LogEntity(msg, color)
    this.add(newLog)
}

private val sdf1 = SimpleDateFormat("MM-dd HH:mm:ss_SSS")

private fun formatMsg(msg: String, tag: String = ""): String {
    val date = Date()
    sdf1.format(date)
    return if (tag.isNotEmpty()) {
        "${sdf1.format(date)}: $tag> $msg \n"
    } else {
        "${sdf1.format(date)}: $msg \n"
    }
}

fun SnapshotStateList<LogEntity>.print(msg: String, tag: String = "", color: Color = Color.Unspecified) {
    this.plus(formatMsg(msg, tag), color)
}

fun SnapshotStateList<LogEntity>.printI(msg: String, tag: String = "") {
    print(msg, tag, Color(73, 156, 84))
}

fun SnapshotStateList<LogEntity>.printW(msg: String, tag: String = "") {
    print(msg, tag, Color(140, 102, 48))
}

fun SnapshotStateList<LogEntity>.printE(msg: String, tag: String = "") {
    print(msg, tag, Color.Red)
}

