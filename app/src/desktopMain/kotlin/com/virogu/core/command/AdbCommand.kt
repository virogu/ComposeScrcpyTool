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
 * @since 2024-03-27 下午 5:19
 **/
class AdbCommand(configStores: ConfigStores) : BaseCommand(configStores) {

    @Volatile
    private var started: Boolean = false

    private val useInner get() = configStores.simpleConfigStore.simpleConfig.value.useInnerAdb

    companion object {
        private val logger = KotlinLogging.logger { }

        fun getSystemAdbPath(workDir: File): String {
            val exeName = if (Common.platform is Platform.Windows) "adb.exe" else "adb"
            val envPath = System.getenv("PATH") ?: ""
            val paths = envPath.split(File.pathSeparator)
            for (path in paths) {
                val cleanPath = path.trim().removeSurrounding("\"")
                if (cleanPath.isEmpty()) continue
                val file = File(cleanPath, exeName)
                if (file.exists() && file.isFile && file.canExecute() && file.absolutePath != File(
                        workDir,
                        exeName
                    ).absolutePath
                ) {
                    return file.absolutePath
                }
            }
            // If not found in PATH, returning this will cause an explicit failure,
            // preventing the OS from silently falling back to the adb.exe in the current working directory.
            return "adb_not_found_in_system_path"
        }
    }

    override val workDir: File by lazy {
        Common.workDir.resolve("app").also {
            logger.debug { "Adb Work Dir: ${it.absolutePath}" }
        }
    }

    private val executable
        get() = if (useInner) innerExe else systemExe

    val systemExe by lazy { arrayOf(getSystemAdbPath(workDir)) }

    private val innerExe by lazy {
        when (Common.platform) {
            is Platform.Linux, is Platform.MacOs -> arrayOf("./adb")
            is Platform.Windows -> {
                // 直接用绝对路径启动，避免经 cmd.exe /c 中转时引号与空格被 cmd 二次解析
                val exe = workDir.resolve("adb.exe")
                if (exe.exists()) {
                    arrayOf(exe.absolutePath)
                } else {
                    arrayOf("cmd.exe", "/c", "adb")
                }
            }
            else -> arrayOf("adb")
        }
    }

    suspend fun adb(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = Common.isDebug,
        redirectFile: File? = null,
        timeout: Long = 5L,
        charset: Charset = Charsets.UTF_8
    ): Result<String> {
        if (!active) {
            return Result.failure(IllegalStateException("adb server is not active"))
        }
        if (!started) {
            showVersion()
            startServer()
        }
        return exec(
            *executable,
            *command,
            env = env,
            redirectFile = redirectFile,
            showLog = showLog,
            consoleLog = consoleLog,
            timeout = timeout,
            outCharset = charset
        )
    }

    /**
     * 带宿主退出码的执行（用于 adb push/pull 的成败判定与设备端标记法命令）。
     * 超时/取消会返回 failure 或向上抛出，不再伪装成空输出成功。
     */
    suspend fun adbChecked(
        vararg command: String,
        timeout: Long = 5L,
        consoleLog: Boolean = Common.isDebug
    ): Result<ShellResult> {
        if (!active) {
            return Result.failure(IllegalStateException("adb server is not active"))
        }
        if (!started) {
            showVersion()
            startServer()
        }
        return execWithExitCode(*executable, *command, consoleLog = consoleLog, timeout = timeout)
    }

    override suspend fun startServer() {
        exec(*executable, "start-server", consoleLog = true)
        started = true
    }

    private suspend fun showVersion() {
        val version = exec(*executable, "version").getOrNull() ?: return
        logger.info { "\n----ADB----\n$version\n----------" }
    }

    override suspend fun killServer() {
        exec(*executable, "kill-server", consoleLog = true)
        started = false
    }

}