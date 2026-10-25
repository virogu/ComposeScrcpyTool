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

package com.virogu.core.command

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import com.virogu.core.config.ConfigStores
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:15
 **/
class HdcCommand(configStores: ConfigStores) : BaseCommand(configStores) {
    @Volatile
    private var started: Boolean = false

    private val useInner get() = configStores.simpleConfigStore.simpleConfig.value.useInnerHdc

    override val workDir: File by lazy {
        Common.workDir.resolve("app").also {
            logger.debug { "Hdc Work Dir: ${it.absolutePath}" }
        }
    }

    private val executable
        get() = if (useInner) innerExe else systemExe

    val systemExe = arrayOf("hdc")

    private val innerExe by lazy {
        when (Common.platform) {
            is Platform.Linux, is Platform.MacOs -> arrayOf("./hdc")
            is Platform.Windows -> arrayOf("cmd.exe", "/c", "hdc")
            else -> arrayOf("hdc")
        }
    }

    suspend fun hdc(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = Common.isDebug,
        timeout: Long = 5L,
        charset: Charset = Charsets.UTF_8
        //charset: Charset = Charset.forName("GBK")
    ): Result<String> {
        if (!active) {
            return Result.failure(IllegalStateException("hdc server is not active"))
        }
        if (!started) {
            startServer()
            showVersion()
        }

        return exec(
            *executable,
            *command,
            env = env,
            showLog = showLog,
            consoleLog = consoleLog,
            timeout = timeout,
            //inputCharset = Charset.forName("GBK"),
            outCharset = charset
        )
    }

    /**
     * 带宿主退出码的执行（用于设备端标记法命令）。
     * 超时/取消会返回 failure 或向上抛出，不再伪装成空输出成功。
     */
    suspend fun hdcChecked(
        vararg command: String,
        timeout: Long = 5L,
        consoleLog: Boolean = Common.isDebug,
        charset: Charset = Charsets.UTF_8
    ): Result<ShellResult> {
        if (!active) {
            return Result.failure(IllegalStateException("hdc server is not active"))
        }
        if (!started) {
            startServer()
            showVersion()
        }
        return execWithExitCode(
            *executable,
            *command,
            consoleLog = consoleLog,
            timeout = timeout,
            outCharset = charset
        )
    }

    override suspend fun startServer() {
        exec(*executable, "start", consoleLog = true)
        started = true
    }

    private suspend fun showVersion() {
        val version = exec(*executable, "version").getOrNull() ?: return
        logger.info { "\n----HDC----\n$version\n----------" }
    }

    override suspend fun killServer() {
        exec(*executable, "kill", consoleLog = true)
        started = false
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}