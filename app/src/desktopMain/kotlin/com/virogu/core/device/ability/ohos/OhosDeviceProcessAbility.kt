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

package com.virogu.core.device.ability.ohos

import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityProcess
import com.virogu.core.device.process.ProcessInfo
import com.virogu.core.device.process.ProcessInfoOhos
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceProcessAbility(private val device: Device) : DeviceAbilityProcess {
    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
    }

    private val target = arrayOf("-t", device.serial)

    override suspend fun refresh(): List<ProcessInfo> {
        //hdc shell "aa dump -a"
        val pidInfo = cmd.hdc(*target, "shell", "ps -ef").getOrNull() ?: return emptyList()
        val bundleInfo = cmd.hdc(*target, "shell", "aa dump -a | grep 'bundle name'").getOrNull() ?: return emptyList()
        return parse(pidInfo = pidInfo, bundleInfo = bundleInfo)
    }

    override suspend fun killProcess(info: ProcessInfo): Result<String> {
        return forceStopProcess(info)
    }

    override suspend fun forceStopProcess(info: ProcessInfo): Result<String> {
        return cmd.hdc(*target, "shell", "aa force-stop ${info.packageName}")
    }

    private fun parse(pidInfo: String, bundleInfo: String): List<ProcessInfoOhos> {
        //UID            PID  PPID C STIME TTY          TIME CMD
        //root             1     0 0 09:37:10 ?     00:00:03 init --second-stage
        //root             2     0 0 09:37:10 ?     00:00:00 [kthreadd]
        //root             3     2 0 09:37:10 ?     00:00:00 [rcu_gp]
        //20010012      1053   264 0 09:37:23 ?     00:00:02 com.example.kikakeyboard:inputMethod
        //20010025      1091   264 0 09:37:24 ?     00:00:02 com.ohos.medialibrary.medialibrarydata
        val pidRegex = Regex("""\s*(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+):*\s*""")
        val pidMap = pidInfo.trim().split("\n").mapNotNull {
            val groupValues = pidRegex.find(it.trim())?.groupValues ?: return@mapNotNull null
            if (groupValues.size < 8) {
                return@mapNotNull null
            }
            val cmd = groupValues[8].split(":").firstOrNull() ?: return@mapNotNull null
            PidInfo(
                user = groupValues[1],
                uid = groupValues[1],
                pid = groupValues[2],
                cmd = cmd
            )
        }.associateBy { it.cmd }
        val bundleRegex = Regex(""".*\[(\S+)].*""")
        return bundleInfo.trim().split("\n").mapNotNull {
            val groupValues = bundleRegex.find(it.trim())?.groupValues ?: return@mapNotNull null
            val bundleName = groupValues[1]
            val pid = pidMap[bundleName] ?: return@mapNotNull null
            ProcessInfoOhos(
                user = pid.user,
                uid = pid.uid,
                pid = pid.pid,
                packageName = bundleName,
            )
        }
    }

    private data class PidInfo(
        val user: String,
        val uid: String,
        val pid: String,
        val cmd: String,
    )

}