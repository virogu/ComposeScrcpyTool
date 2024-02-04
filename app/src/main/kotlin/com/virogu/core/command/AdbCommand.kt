package com.virogu.core.command

import com.virogu.core.commonWorkDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:19
 **/
class AdbCommand : BaseCommand() {
    override val workDir: File by lazy {
        commonWorkDir.resolve("app").also {
            logger.debug("Adb Work Dir: ${it.absolutePath}")
        }
    }

    suspend fun adb(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = false,
        timeout: Long = 10L,
        charset: Charset = Charsets.UTF_8
    ): Result<String> {
        return exec(
            "adb",
            *command,
            env = env,
            showLog = showLog,
            consoleLog = consoleLog,
            timeout = timeout,
            charset = charset
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}