package com.virogu.tools.config

import com.virogu.bean.Configs
import com.virogu.tools.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream

abstract class BaseConfigTool : ConfigTool {

    protected open val configFile: File = File("app/config", "app_preferences")

    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    final override val configsFlow: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    protected var updateJob: Job? = null

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
        updateJob?.cancel()
        updateJob = scope.launch {
            val new = configsFlow.value.toMutableMap().apply {
                runCatching {
                    val s = json.encodeToString<T>(value)
                    put(key, s)
                }
            }
            configsFlow.emit(new)
            delay(3000)
            println("start save config to file")
            runCatching {
                if (!configFile.exists()) {
                    configFile.parentFile.mkdirs()
                    configFile.createNewFile()
                }
                val s = json.encodeToString(Configs(new))
                configFile.writeText(s)
            }.onSuccess {
                println("save config to file success")
            }.onFailure {
                println(it)
            }
        }
    }

}