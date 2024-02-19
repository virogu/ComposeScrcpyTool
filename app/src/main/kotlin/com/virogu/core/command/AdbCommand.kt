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
 * @since 2024-03-27 下午 5:19
 **/
class AdbCommand : BaseCommand() {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override val workDir: File by lazy {
        commonWorkDir.resolve("app").also {
            logger.debug("Adb Work Dir: ${it.absolutePath}")
        }
    }

    private val executable by lazy {
        when (currentPlateForm) {
            is PlateForm.Linux -> arrayOf("./adb")
            else -> arrayOf("cmd.exe", "/c", "adb")
        }
    }

    suspend fun adb(
        vararg command: String,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = isDebug,
        timeout: Long = 10L,
        charset: Charset = Charsets.UTF_8
    ): Result<String> {
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
}