package com.virogu.tools

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

val logger: Logger by lazy {
    LoggerFactory.getLogger("CommonLogger")
}

sealed class PlateForm(info: String, version: String) {
    data class Windows(val info: String, val version: String) : PlateForm(info, version)
    data class Linux(val info: String, val version: String) : PlateForm(info, version)
    data class MacOs(val info: String, val version: String) : PlateForm(info, version)
    data class Unknown(val info: String, val version: String) : PlateForm(info, version)
}

val currentOsName: String by lazy {
    System.getProperty("os.name")
}

val currentOsVersion: String by lazy {
    System.getProperty("os.version")
}

val currentPlateForm by lazy {
    when {
        currentOsName.contains("windows", true) -> PlateForm.Windows(currentOsName, currentOsVersion)
        currentOsName.contains("linux", true) -> PlateForm.Linux(currentOsName, currentOsVersion)
        currentOsName.contains("mac", true) -> PlateForm.MacOs(currentOsName, currentOsVersion)
        else -> PlateForm.Unknown(currentOsName, currentOsVersion)
    }
}

val pingCommand: Array<String>? by lazy {
    when (currentPlateForm) {
        is PlateForm.Windows -> arrayOf("ping", "-n", "1")
        is PlateForm.Linux -> arrayOf("ping", "-c", "1")
        else -> null
    }
}

val commonResourceDir: File by lazy {
    File(System.getProperty("compose.application.resources.dir")).also {
        logger.info("workFile: ${it.absolutePath}")
    }.absoluteFile
}

val commonWorkDir: File by lazy {
    val file = when (currentPlateForm) {
        is PlateForm.Windows -> commonResourceDir

        is PlateForm.Linux -> projectDataDir.also {
            it.runCatching {
                if (!exists()) {
                    mkdirs()
                }
            }
        }

        else -> commonResourceDir
    }
    file.absoluteFile
}

val userRootConfigDir: File by lazy {
    val userDir = System.getProperty("user.home")
    val file = when (currentPlateForm) {
        is PlateForm.Windows -> File(userDir, "AppData/Roaming")
        is PlateForm.Linux -> File(userDir, ".config")
        else -> File(userDir, "config")
    }
    file
}

val projectDataDir: File by lazy {
    File(userRootConfigDir, "scrcpy-tool")
}