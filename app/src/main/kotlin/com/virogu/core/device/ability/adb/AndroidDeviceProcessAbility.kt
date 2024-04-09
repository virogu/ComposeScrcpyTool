package com.virogu.core.device.ability.adb

import com.virogu.core.command.AdbCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.process.ProcessInfo
import com.virogu.core.device.process.ProcessInfoAndroid
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
        return refresh1()
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

    private suspend fun refresh1(): List<ProcessInfo> {
        return cmd.adb(
            "-s", device.serial,
            "shell", "am dump -a | grep 'PID #'"
        ).map(::parse1).getOrNull().orEmpty()
    }

    private suspend fun refresh2(): List<ProcessInfo> {
        return cmd.adb(
            "-s", device.serial,
            "shell", "dumpsys activity processes"
        ).map(::parse2).getOrNull().orEmpty()
    }

    //am dump -a | grep 'PID #'
    private fun parse1(info: String): List<ProcessInfoAndroid> {
        //PID #4466: ProcessRecord{35f4c1b 4466:com.android.keychain/1000}
        //PID #4494: ProcessRecord{9b3c996 4494:com.android.tv.settings/1000}
        val pidRegex = Regex("""PID #(\d+):\s+ProcessRecord\{\S+\s+\d+:(\S+)/(\S+)}""")
        return info.trim().split("\n").mapNotNull {
            val r = pidRegex.find(it.trim())?.groupValues ?: return@mapNotNull null
            if (r.size < 3) {
                return@mapNotNull null
            }
            val pid = r[1]
            val uid = r[3]
            val packageName = r[2]
            ProcessInfoAndroid(
                user = uid,
                uid = uid,
                pid = pid,
                processName = packageName,
                packageName = packageName,
            )
        }
    }

    //dumpsys activity processes
    private fun parse2(info: String): List<ProcessInfoAndroid> = info.runCatching {
        val matches = Regex("""(?s)(APP|PERS)\*\s+(.*?)(\*|PID mappings:)""").findAll(this)
        if (matches.count() <= 0) {
            return emptyList()
        }
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
            ProcessInfoAndroid(
                user = user,
                uid = uid,
                pid = pid,
                processName = processName,
                packageName = packageName,
                params = maps
            )
        }.toList()
    }.getOrNull().orEmpty()

}