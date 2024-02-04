package com.virogu.core.device.ability.adb

import com.virogu.core.bean.ProcessInfo
import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityProcess
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class AndroidDeviceProcessAbility(private val device: Device) : DeviceAbilityProcess {
    companion object {
        private val cmd: AdbCommand by DI.global.instance<AdbCommand>()
    }

    override suspend fun refresh(): List<ProcessInfo> {
        return cmd.adb(
            "-s", device.serial, "shell", "dumpsys activity processes"
        ).map {
            it.parseProcess()
        }.getOrNull().orEmpty()
    }

    override suspend fun killProcess(info: ProcessInfo): Result<String> {
        return cmd.adb(
            "-s", device.serial, "shell",
            "am", "kill", info.packageName,
            consoleLog = true
        )
    }

    override suspend fun forceStopProcess(info: ProcessInfo): Result<String> {
        return cmd.adb(
            "-s", device.serial, "shell",
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