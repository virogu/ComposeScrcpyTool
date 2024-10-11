package com.virogu.core.command

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:15
 **/
class HdcCommand : BaseCommand() {
    @Volatile
    private var started: Boolean = false

    override val workDir: File by lazy {
        Common.workDir.resolve("app").also {
            logger.debug("Hdc Work Dir: ${it.absolutePath}")
        }
    }

    private val executable by lazy {
        when (Common.platform) {
            is Platform.Linux -> arrayOf("./hdc")
            else -> arrayOf("cmd.exe", "/c", "hdc")
        }
    }

    suspend fun hdc(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = Common.isDebug,
        timeout: Long = 5L,
        charset: Charset = Charset.forName("GBK")
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
            charset = charset
        )
    }

    override suspend fun startServer() {
        exec(*executable, "start", consoleLog = true)
        started = true
    }

    private suspend fun showVersion() {
        val version = exec(*executable, "version").getOrNull() ?: return
        logger.info("\n----HDC----\n$version\n----------")
    }

    override suspend fun killServer() {
        exec(*executable, "kill", consoleLog = true)
        started = false
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}