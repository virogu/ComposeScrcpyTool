package com.virogu.core.tool.special.adb

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.ProcessInfo
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.special.ProcessManage

class ProcessManageAdb(
    private val progressTool: ProgressTool
) : ProcessManage {

    override suspend fun refresh(device: DeviceInfo): List<ProcessInfo> {
        return progressTool.exec(
            "adb", "-s", device.serial, "shell", "dumpsys activity processes"
        ).map {
            it.parseProcess()
        }.getOrNull().orEmpty()
    }

    override suspend fun killProcess(device: DeviceInfo, info: ProcessInfo): Result<String> {
        return progressTool.exec(
            "adb", "-s", device.serial, "shell",
            "am", "kill", info.packageName,
            consoleLog = true
        )
    }

    override suspend fun forceStopProcess(device: DeviceInfo, info: ProcessInfo): Result<String> {
        return progressTool.exec(
            "adb", "-s", device.serial, "shell",
            "am", "force-stop", info.packageName,
            consoleLog = true
        )
    }

    private fun String.parseProcess(): List<ProcessInfo> {
        val matches = Regex("""(?s)(APP|PERS)\*\s+(.*?)(\*|PID mappings:)""").findAll(this)
        if (matches.count() <= 0) {
            return emptyList()
        }
        return try {
            matches.mapNotNull { matchesResult ->
                val process = matchesResult.groupValues[2]
                val first = process.reader().readLines().firstOrNull()?.trim() ?: return@mapNotNull null
                //UID 1000 ProcessRecord{a0f6d00 727:com.microsoft.windows.systemapp/u0a48}
                val baseInfo = Regex("""\S+\s+(\d+)\s+ProcessRecord\{(\S+)\s+(\d+):(\S+)/(\S+)}(.*)""").find(first)
                    ?: return@mapNotNull null
                val uid = baseInfo.groupValues.getOrNull(1) ?: return@mapNotNull null
                val pid = baseInfo.groupValues.getOrNull(3) ?: return@mapNotNull null
                val processName = baseInfo.groupValues.getOrNull(4) ?: return@mapNotNull null
                val packageName = processName.split(":").firstOrNull() ?: return@mapNotNull null
                val user: String = baseInfo.groupValues.getOrNull(5).orEmpty().let {
                    if (it.first().isDigit()) {
                        it.toIntOrNull()?.toString() ?: "0"
                    } else {
                        val m = Regex("""(.*?)(\d+)(.*?)""").find(it)
                        m?.groupValues?.getOrNull(2) ?: "0"
                    }
                }
                val maps = Regex("""\s*(\w+)=(\{[^{}]*}|\S+)\s*""").findAll(process).associate { kv ->
                    kv.groupValues[1] to kv.groupValues[2]
                }
                ProcessInfo(
                    user = user,
                    uid = uid,
                    pid = pid,
                    processName = processName,
                    packageName = packageName,
                    params = maps
                )
            }.toList()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

}