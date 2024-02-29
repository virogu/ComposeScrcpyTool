package com.virogu.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

const val isDebug = false

val commonLogger: Logger by lazy {
    LoggerFactory.getLogger("CommonLogger")
}

sealed class PlateForm(open val info: String, open val version: String) {
    data class Windows(override val info: String, override val version: String) : PlateForm(info, version)
    data class Linux(override val info: String, override val version: String) : PlateForm(info, version)
    data class MacOs(override val info: String, override val version: String) : PlateForm(info, version)
    data class Unknown(override val info: String, override val version: String) : PlateForm(info, version)
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
    val resourcesDir = System.getProperty("compose.application.resources.dir").orEmpty()
    File(resourcesDir).also {
        commonLogger.info("ResourceDir: ${it.absolutePath}")
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
        is PlateForm.MacOs -> File(userDir, "Library/Application Support")
        else -> File(userDir, "config")
    }
    file
}

val projectDataDir: File by lazy {
    File(userRootConfigDir, "scrcpy-tool")
}

fun MutableMap<String, String>.appendCommonEnv(): MutableMap<String, String> {
    //put("LANG", "en_US.UTF-8")
    //put("LC_ALL", "en_US.UTF-8")
    //val (key, split) = when (currentPlateForm) {
    //    is PlateForm.Windows -> Pair("Path", ";")
    //    is PlateForm.Linux -> Pair("PATH", ":")
    //    else -> return this
    //}
    //val path = buildString {
    //    append(File(commonWorkDir, "app").absolutePath)
    //    get(key)?.let {
    //        append(split)
    //        append(it)
    //    }
    //}
    //put(key, path)
    return this
}