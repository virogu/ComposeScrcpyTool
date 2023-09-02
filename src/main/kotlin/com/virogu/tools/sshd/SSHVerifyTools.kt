package com.virogu.tools.sshd

import com.virogu.bean.DeviceSshConfig
import com.virogu.tools.commonLogger
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
        this.user = user
    }

    fun updatePwd(pwd: String) {
        this.pwd = pwd
    }

    fun updatePPk(path: File, ppk: List<DeviceSshConfig.PPK>) {
        this.ppk = ppk.mapNotNull {
            val f = File(path, it.name)
            if (f.exists()) {
                PPKFile(f, it.pwd)
            } else {
                commonLogger.warn("ppk file [$f] not exist.")
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
                    commonLogger.info("load key [${ppk.path}] success.")
                }.onFailure {
                    commonLogger.warn("load key [${ppk.path}] fail.")
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