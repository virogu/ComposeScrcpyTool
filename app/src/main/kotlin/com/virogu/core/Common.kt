package com.virogu.core

import com.virogu.core.bean.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.BuildConfig
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
object Common {
    val logger: Logger by lazy {
        LoggerFactory.getLogger("CommonLogger")
    }

    val isDebug: Boolean by lazy {
        File(projectHomeDir, "debug").exists()
    }

    val osVersion: String by lazy {
        get("os.version")
    }

    val osName: String by lazy {
        get("os.name")
    }

    val resourcePath: String by lazy {
        get("compose.application.resources.dir")
    }

    val resourceDir: File by lazy {
        File(resourcePath).also {
            logger.info("ResourceDir: ${it.absolutePath}")
        }.absoluteFile
    }

    val workDir: File by lazy {
        val file = when (platform) {
            is Platform.Windows -> resourceDir

            is Platform.Linux -> projectDataDir.also {
                it.runCatching {
                    if (exists() && !isDirectory) {
                        deleteRecursively()
                    }
                    if (!exists()) {
                        mkdirs()
                    }
                }
            }

            else -> resourceDir
        }
        file.absoluteFile
    }

    val projectDataDir: File by lazy {
        projectHomeDir.resolve("data")
    }

    val projectHomeDir: File by lazy {
        userHomeDir.resolve(PROJECT_NAME)
    }

    val projectConfigDir: File by lazy {
        projectHomeDir.resolve("config").also {
            if (it.exists() && !it.isDirectory) {
                it.deleteRecursively()
            }
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    val projectTmpDir: File by lazy {
        projectHomeDir.resolve("tmp").also {
            it.deleteRecursively()
            logger.debug("clear tmp dir: ${it.path}")
            it.listFiles()?.also { files ->
                Common.logger.info("tmp residual files: \n${files.map { f -> f.name }}")
            }
            it.mkdirs()
            logger.debug("mkdir tmp dir: ${it.path}")
        }
    }

    val platform by lazy {
        val osName = Common.osName
        val osVersion = Common.osVersion
        when {
            osName.contains("windows", true) -> Platform.Windows(osName, osVersion)
            osName.contains("linux", true) -> Platform.Linux(osName, osVersion)
            osName.contains("mac", true) -> Platform.MacOs(osName, osVersion)
            else -> Platform.Unknown(osName, osVersion)
        }
    }

    private const val PROJECT_NAME = BuildConfig.AppName

    private val userHomeDir: File by lazy {
        val userHome = get("user.home")
        val file = when (platform) {
            is Platform.Windows -> File(userHome, "AppData/Roaming")
            is Platform.Linux -> File(userHome, ".config")
            is Platform.MacOs -> File(userHome, "Library/Application Support")
            else -> File(userHome, "config")
        }
        file
    }

    fun get(key: String): String {
        return System.getProperty(key).orEmpty()
    }
}