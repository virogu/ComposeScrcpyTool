package com.virogu.core.tool.ssh

import com.virogu.core.Common
import com.virogu.core.bean.DeviceSshConfig
import org.apache.sshd.putty.PuttyKeyUtils
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.security.KeyPair

object SSHVerifyTools {
    private var ppk: List<PPKFile> = emptyList()
    var user: String = ""
        private set
    var pwd: String = ""
        private set
    var keyPairs: List<KeyPair> = emptyList()
        private set

    fun updateUser(user: String) {
        SSHVerifyTools.user = user
    }

    fun updatePwd(pwd: String) {
        SSHVerifyTools.pwd = pwd
    }

    fun updatePPk(path: File, ppk: List<DeviceSshConfig.PPK>) {
        SSHVerifyTools.ppk = ppk.mapNotNull {
            val f = File(path, it.name)
            if (f.exists()) {
                PPKFile(f, it.pwd)
            } else {
                Common.logger.warn { "ppk file [$f] not exist." }
                null
            }
        }
        updateKeyPairs()
    }

    private fun updateKeyPairs() {
        keyPairs = mutableListOf<KeyPair>().apply {
            ppk.forEach { ppk ->
                runCatching {
                    PuttyKeyUtils.DEFAULT_INSTANCE.loadKeyPairs(null, ppk.path, { _, _, _ ->
                        ppk.pwd
                    }).also(::addAll)
                }.onSuccess {
                    Common.logger.info { "load key [${ppk.path}] success." }
                }.onFailure {
                    Common.logger.warn { "load key [${ppk.path}] fail." }
                }
            }
        }
    }

    internal data class PPKFile(
        val file: File,
        val pwd: String
    ) {
        val path: Path = FileSystems.getDefault().getPath(file.absolutePath)
        val resourceKey = path.toString()
    }
}