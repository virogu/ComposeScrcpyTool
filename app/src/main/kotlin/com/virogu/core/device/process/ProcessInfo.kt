package com.virogu.core.device.process

interface ProcessInfo {
    val user: String
    val uid: String
    val pid: String
    val processName: String
    val packageName: String
    val abi: String

    sealed class SortBy(
        val tag: String,
        val sort: (list: Iterable<ProcessInfo>, desc: Boolean) -> List<ProcessInfo>
    ) {
        data object NAME : SortBy("NAME", { list, desc ->
            if (desc) {
                list.sortedByDescending {
                    it.processName
                }
            } else {
                list.sortedBy {
                    it.processName
                }
            }
        })

        data object PID : SortBy("PID", { list, desc ->
            if (desc) {
                list.sortedByDescending {
                    it.pid.toLongOrNull() ?: 0
                }
            } else {
                list.sortedBy {
                    it.pid.toLongOrNull() ?: 0
                }
            }
        })
    }
}