package com.virogu.tools.config

import com.virogu.bean.Configs
import com.virogu.tools.json
import com.virogu.tools.projectDataDir
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

abstract class BaseConfigTool : ConfigTool {

    protected open val configFile: File = File(projectDataDir, "config/app_preferences")

    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    final override val configsFlow: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    protected var writeConfigJob: Job? = null

    init {
        @OptIn(ExperimentalSerializationApi::class)
        scope.launch {
            val config: Configs = try {
                if (!configFile.exists() || !configFile.isFile) {
                    throw IllegalArgumentException("config path not exit")
                }
                json.decodeFromStream(FileInputStream(configFile))
            } catch (e: Throwable) {
                Configs()
            }
            configsFlow.emit(config.value)
        }
    }

    protected inline fun <reified T> getConfig(
        key: String,
        defaultValue: T? = null
    ) = configsFlow.value.getConfig(key, defaultValue)

    protected inline fun <reified T> Map<String, String>.getConfig(
        key: String,
        defaultValue: T? = null
    ): T? {
        return this[key]?.let {
            try {
                json.decodeFromString<T>(it)
            } catch (e: Throwable) {
                null
            }
        } ?: defaultValue
    }

    protected inline fun <reified T> getConfigNotNull(
        key: String,
        defaultValue: T
    ): T = configsFlow.value.getConfigNotNull(key, defaultValue)

    protected inline fun <reified T> Map<String, String>.getConfigNotNull(
        key: String,
        defaultValue: T
    ): T = getConfig(key, defaultValue)!!

    protected inline fun <reified T> updateConfig(key: String, value: T) {
        writeConfigJob?.cancel()
        writeConfigJob = scope.launch {
            val new = configsFlow.value.toMutableMap().apply {
                runCatching {
                    val s = json.encodeToString<T>(value)
                    put(key, s)
                }
            }
            configsFlow.emit(new)
            delay(3000)
            writeConfigToFile()
        }
    }

    protected suspend fun writeConfigToFile() = withContext(Dispatchers.IO) {
        runCatching {
            if (!configFile.exists()) {
                configFile.parentFile.mkdirs()
                configFile.createNewFile()
            }
            val s = json.encodeToString<Configs>(Configs(configsFlow.value))
            configFile.writeText(s)
            println("config updated")
        }.onFailure {
            println(it)
            if (it is IOException) {
                logger.warn("无法保存配置，请尝试以管理员身份运行")
            }
        }
    }

    final override fun writeConfigNow() {
        if (writeConfigJob?.isActive != true) {
            return
        }
        writeConfigJob?.cancel()
        runBlocking {
            writeConfigToFile()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BaseConfigTool::class.java)
    }

}