package com.virogu.core.tool.init

import com.virogu.core.command.BaseCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

class InitToolLinux : InitToolDefault() {

    private val cmd: BaseCommand by DI.global.instance<BaseCommand>()

    override fun doInit() {
        runBlocking {
            withContext(Dispatchers.IO) {
                innerInit()
            }
        }
    }

    private suspend fun innerInit() = runCatching {
        prepareResource(resourceDir, workDir, "")
        listOf(
            File(workDir, "app/adb"),
            File(workDir, "app/scrcpy"),
        ).forEach { f ->
            chmodX(f.absolutePath)
        }
        initStateFlow.emit(InitTool.State(true))
    }

    private fun prepareResource(origin: File, target: File, child: String) {
        println("prepare resource: [$child]")
        File(resourceDir, child).listFiles()?.forEach {
            if (it.isDirectory) {
                prepareResource(origin, target, "$child/${it.name}")
            } else if (it.isFile) {
                val f = File(workDir, "$child/${it.name}")
                if (!f.exists()) {
                    it.copyTo(f, true)
                    println("copy file [$it] to [$f]")
                } else if (f.md5() != it.md5()) {
                    f.delete()
                    it.copyTo(f, true)
                    println("[$f] exist, but md5 is not same, re-copy [$it] to [$f]")
                }
            }
        }
    }

    private suspend fun chmodX(absolutePath: String) {
        val runnable = cmd.exec(
            "sh", "-c", "test -x '${absolutePath}' && echo '1' || echo '0'",
        ).fold({
            println("test -x ${absolutePath}, result: [$it]")
            it.trim() == "1"
        }, {
            println("[$it]")
            false
        })
        if (runnable) {
            return
        }
        cmd.exec(
            "chmod", "+x", absolutePath
        ).onSuccess {
            println("chmod +x ${absolutePath}, result: [$it]")
        }.onFailure {
            println("chmod +x ${absolutePath}, result: [$it]")
        }
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