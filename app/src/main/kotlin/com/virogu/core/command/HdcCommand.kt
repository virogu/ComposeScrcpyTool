package com.virogu.core.command

import com.virogu.core.PlateForm
import com.virogu.core.commonWorkDir
import com.virogu.core.currentPlateForm
import com.virogu.core.isDebug
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
        commonWorkDir.resolve("app").also {
            logger.debug("Hdc Work Dir: ${it.absolutePath}")
        }
    }

    private val executable by lazy {
        when (currentPlateForm) {
            is PlateForm.Linux -> arrayOf("./hdc")
            else -> arrayOf("cmd.exe", "/c", "hdc")
        }
    }

    suspend fun hdc(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = isDebug,
        timeout: Long = 5L,
        charset: Charset = Charset.forName("GBK")
    ): Result<String> {
        if (!active) {
            return Result.failure(IllegalStateException("hdc server is not active"))
        }
        if (!started) {
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
        exec(*executable, "start", consoleLog = true)
        started = true
    }

    override suspend fun killServer() {
        exec(*executable, "kill", consoleLog = true)
        started = false
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}