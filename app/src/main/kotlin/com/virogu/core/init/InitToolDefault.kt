package com.virogu.core.init

import com.virogu.core.bean.DeviceSshConfig
import com.virogu.core.commonLogger
import com.virogu.core.commonResourceDir
import com.virogu.core.commonWorkDir
import com.virogu.core.json
import com.virogu.core.tool.impl.SSHVerifyTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

open class InitToolDefault : InitTool {

    override val workDir: File by lazy {
        commonWorkDir
    }

    override val resourceDir: File by lazy {
        commonResourceDir
    }

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState(true))

    override fun init() {
        val t = System.currentTimeMillis()
        commonLogger.info("InitTool init")
        doInit()
        afterInit()
        commonLogger.info("InitTool init finish, spend ${System.currentTimeMillis() - t}ms")
    }

    protected open fun doInit() {
    }

    protected open fun afterInit() {
        runBlocking {
            initSSHConfig()
        }
    }

    protected open suspend fun initSSHConfig() = withContext(Dispatchers.IO) {
        val configFile = File(workDir, "files/ssh/device_ssh_config.json")
        if (!configFile.exists()) {
            commonLogger.info("ssh config file [$configFile] not exits")
            return@withContext
        }
        val config: DeviceSshConfig = runCatching {
            json.decodeFromString<DeviceSshConfig>(configFile.readText())
        }.getOrNull() ?: return@withContext
        val ppkPath = File(workDir, "files/ssh")
        config.ppk.forEach {
            if (it.name.isEmpty() || it.value.isEmpty()) {
                return@forEach
            }
            val f = File(ppkPath, it.name)
            if (!f.exists()) {
                commonLogger.info("init ppk [${it.name}]")
                f.writeText(it.value)
            }
        }
        SSHVerifyTools.updateUser(config.user)
        SSHVerifyTools.updatePwd(config.pwd)
        SSHVerifyTools.updatePPk(ppkPath, config.ppk)
    }

}