package com.virogu.tools.init

import com.virogu.bean.DeviceSshConfig
import com.virogu.tools.commonResourceDir
import com.virogu.tools.commonWorkDir
import com.virogu.tools.json
import com.virogu.tools.logger
import com.virogu.tools.sshd.SSHVerifyTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.File

open class DefaultInitTool : InitTool {

    override val workDir: File by lazy {
        commonWorkDir
    }

    override val resourceDir: File by lazy {
        commonResourceDir
    }

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState(true))

    override fun init() {
        val t = System.currentTimeMillis()
        logger.info("InitTool init")
        doInit()
        afterInit()
        logger.info("InitTool init finish, spend ${System.currentTimeMillis() - t}ms")
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
            logger.info("ssh config file [$configFile] not exits")
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
                logger.info("init ppk [${it.name}]")
                f.writeText(it.value)
            }
        }
        SSHVerifyTools.updateUser(config.user)
        SSHVerifyTools.updatePwd(config.pwd)
        SSHVerifyTools.updatePPk(ppkPath, config.ppk)
    }

}