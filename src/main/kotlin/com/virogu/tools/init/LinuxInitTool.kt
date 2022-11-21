package com.virogu.tools.init

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.commonResourceDir
import com.virogu.tools.commonWorkDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

class LinuxInitTool(
    private val progressTool: ProgressTool
) : InitTool {

    private val workDir: File by lazy {
        commonWorkDir
    }

    private val resourceDir: File by lazy {
        commonResourceDir
    }

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState(false))

    override fun init() {
        val t = System.currentTimeMillis()
        runBlocking {
            withContext(Dispatchers.IO) {
                innerInit()
            }
        }
        println("init spend ${System.currentTimeMillis() - t}ms")
    }

    private suspend fun innerInit() = runCatching {
        File(resourceDir, "app").listFiles()?.forEach {
            val f = File(workDir, "app/${it.name}")
            if (!f.exists()) {
                it.copyTo(f, true)
                println("copy [$it] to [$f]")
            } else if (f.md5() != it.md5()) {
                f.delete()
                it.copyTo(f, true)
                println("[$f] exist, but md5 is not same, re-copy [$it] to [$f]")
            }
        }
        File(resourceDir, "files").listFiles()?.forEach {
            val f = File(workDir, "files/${it.name}")
            if (!f.exists()) {
                it.copyTo(f, true)
                println("copy [$it] to [$f]")
            }
        }
        listOf(
            File(workDir, "app/adb"),
            File(workDir, "app/scrcpy"),
        ).forEach { f ->
            val runnable = progressTool.exec(
                "sh", "-c", "test -x '${f.absolutePath}' && echo '1' || echo '0'",
            ).fold({
                println("test -x ${f.absolutePath}, result: [$it]")
                it.trim() == "1"
            }, {
                println("[$it]")
                false
            })
            if (runnable) {
                return@forEach
            }
            progressTool.exec(
                "chmod", "+x", f.absolutePath
            ).onSuccess {
                println("chmod +x ${f.absolutePath}, result: [$it]")
            }.onFailure {
                println("chmod +x ${f.absolutePath}, result: [$it]")
            }
        }
        initStateFlow.emit(InitState(true))
    }

    private fun File.md5(): String {
        val md5Digest = MessageDigest.getInstance("MD5")
        this.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead = inputStream.read(buffer)

            while (bytesRead != -1) {
                md5Digest.update(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }

            val md5Bytes = md5Digest.digest()
            val md5BigInteger = BigInteger(1, md5Bytes)
            var md5Hash = md5BigInteger.toString(16)

            while (md5Hash.length < 32) {
                md5Hash = "0$md5Hash"
            }
            return md5Hash
        }
    }

}