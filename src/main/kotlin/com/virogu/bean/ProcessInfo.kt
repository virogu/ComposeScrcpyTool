package com.virogu.bean

data class ProcessInfo(
    val pid: String,
    val name: String,
    val cpuRate: String
) {

    sealed class SortBy(
        val selector: (ProcessInfo) -> String
    ) {
        object NAME : SortBy({ it.name })
        object PID : SortBy({ it.pid })
        object CPU : SortBy({ it.cpuRate })
    }

    enum class SortType {
        ASC, DESC
    }
}