package com.virogu.bean

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

