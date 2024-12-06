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

package com.virogu.core.tool.init

import com.virogu.core.Common
import com.virogu.core.bean.DeviceSshConfig
import com.virogu.core.json
import com.virogu.core.tool.ssh.SSHVerifyTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

open class InitToolDefault : InitTool {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val workDir: File by lazy {
        Common.workDir
    }

    override val resourceDir: File by lazy {
        Common.resourceDir
    }

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState.Default)

    override fun init() {
        scope.launch {
            val t = System.currentTimeMillis()
            Common.logger.info { "init..." }
            runCatching {
                doInit()
            }
            afterInit()
            Common.logger.info { "init finish, spend ${System.currentTimeMillis() - t}ms" }
            initStateFlow.emit(InitState.Success)
        }
    }

    protected open suspend fun doInit() {
    }

    private suspend fun afterInit() = runCatching {
        initSSHConfig()
    }

    protected open suspend fun initSSHConfig() {
        val configFile = File(workDir, "files/ssh/device_ssh_config.json")
        if (!configFile.exists()) {
            Common.logger.info { "ssh config file [$configFile] not exits" }
            return
        }
        val config: DeviceSshConfig = runCatching {
            json.decodeFromString<DeviceSshConfig>(configFile.readText())
        }.getOrNull() ?: return
        val ppkPath = File(workDir, "files/ssh")
        config.ppk.forEach {
            if (it.name.isEmpty() || it.value.isEmpty()) {
                return@forEach
            }
            val f = File(ppkPath, it.name)
            if (!f.exists()) {
                Common.logger.info { "init ppk [${it.name}]" }
                f.writeText(it.value)
            }
        }
        SSHVerifyTools.updateUser(config.user)
        SSHVerifyTools.updatePwd(config.pwd)
        SSHVerifyTools.updatePPk(ppkPath, config.ppk)
    }

}