package com.virogu.tools.sshd

import com.virogu.tools.commonWorkDir
import org.apache.sshd.putty.PuttyKeyUtils
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.security.KeyPair

object SSHVerifyTools {

    private val workDir: File by lazy {
        commonWorkDir
    }

    internal data class PPKFile(
        val file: File,
        val pwd: String
    ) {
        val path: Path = FileSystems.getDefault().getPath(file.absolutePath)
        val resourceKey = path.toString()
    }

    private val ppkPaths: List<PPKFile> by lazy {
        listOf()
    }

    val keyPairs: List<KeyPair> by lazy {
        mutableListOf<KeyPair>().apply {
            ppkPaths.forEach { ppk ->
                PuttyKeyUtils.DEFAULT_INSTANCE.loadKeyPairs(null, ppk.path, { _, _, _ ->
                    ppk.pwd
                }).also(::addAll)
            }
        }
    }

    const val commonSShPwd = ""

}