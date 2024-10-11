package com.virogu.core.command

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:19
 **/
class AdbCommand : BaseCommand() {

    @Volatile
    private var started: Boolean = false

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override val workDir: File by lazy {
        Common.workDir.resolve("app").also {
            logger.debug("Adb Work Dir: ${it.absolutePath}")
        }
    }

    private val executable by lazy {
        when (Common.platform) {
            is Platform.Linux -> arrayOf("./adb")
            else -> arrayOf("cmd.exe", "/c", "adb")
        }
    }

    suspend fun adb(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = Common.isDebug,
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
            showLog = showLog,
            consoleLog = consoleLog,
            timeout = timeout,
            charset = charset
        )
    }

    override suspend fun startServer() {
        exec(*executable, "start-server", consoleLog = true)
        started = true
    }

    private suspend fun showVersion() {
        val version = exec(*executable, "version").getOrNull() ?: return
        logger.info("\n----ADB----\n$version\n----------")
    }

    override suspend fun killServer() {
        exec(*executable, "kill-server", consoleLog = true)
        started = false
    }

}