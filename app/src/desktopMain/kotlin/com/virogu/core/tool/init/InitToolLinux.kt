/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.tool.init

import com.virogu.core.command.BaseCommand
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

open class InitToolLinux : InitToolDefault() {

    private val cmd: BaseCommand by DI.global.instance<BaseCommand>()

    protected open val appList by lazy {
        listOf(
            File(workDir, "app/adb"),
            File(workDir, "app/scrcpy"),
            File(workDir, "app/scrcpy_bin"),
            File(workDir, "app/hdc"),
        )
    }

    override suspend fun doInit() {
        innerInit()
    }

    private suspend fun innerInit() = runCatching {
        prepareResource(resourceDir, workDir, "")
        appList.forEach { f ->
            chmodX(f.absolutePath)
        }
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

    private suspend fun chmodX(absolutePath: String) = runCatching {
        //val runnable = cmd.exec("test", "-x", "'${absolutePath}' && echo '1' || echo '0'").fold({
        //    println("test -x ${absolutePath}, result: [$it]")
        //    it.trim() == "1"
        //}, {
        //    println("test error [$it]")
        //    false
        //})
        //if (runnable) {
        //    return@runCatching
        //}
        cmd.exec("sh", "-c", "chmod +x '${absolutePath}'").onSuccess {
            println("chmod +x ${absolutePath}, success: [$it]")
        }.onFailure {
            println("chmod +x ${absolutePath}, fail: [$it]")
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