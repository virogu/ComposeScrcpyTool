package com.virogu.core.command

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import io.github.oshai.kotlinlogging.KotlinLogging
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
            logger.debug { "Hdc Work Dir: ${it.absolutePath}" }
        }
    }

    private val executable by lazy {
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
            inputCharset = Charset.forName("GBK"),
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