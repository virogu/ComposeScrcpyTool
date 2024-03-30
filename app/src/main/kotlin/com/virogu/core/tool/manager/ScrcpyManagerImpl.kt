package com.virogu.core.tool.manager

import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.commonWorkDir
import com.virogu.core.device.Device
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ScrcpyManagerImpl : ScrcpyManager {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val scrcpyMap = HashMap<String, Process>()

    private val workDir: File by lazy {
        File(commonWorkDir, "app")
    }

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val activeDevicesFLow = MutableStateFlow<Set<String>>(emptySet())

    override fun connect(
        device: Device,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ) {
        val serial = device.serial
        withLock {
            if (scrcpyMap[serial] != null) {
                return@withLock
            }
            val progress = device.scrcpyAbility.connect(
                this, commonConfig, config
            ) ?: run {
                return@withLock
            }
            progress.onExit().thenApply {
                scope.launch {
                    mutex.withLock {
                        scrcpyMap.remove(serial)
                        activeDevicesFLow.emit(scrcpyMap.keys.toSet())
                    }
                }
            }
            if (progress.isAlive) {
                scrcpyMap[serial] = progress
                activeDevicesFLow.emit(scrcpyMap.keys.toSet())
            }
        }
    }

    override fun disConnect(device: Device?) {
        withLock {
            if (device == null) {
                scrcpyMap.forEach { (_, progress) ->
                    progress.destroyRecursively()
                }
                scrcpyMap.clear()
            } else {
                if (!scrcpyMap.containsKey(device.serial)) {
                    return@withLock
                }
                scrcpyMap.remove(device.serial)?.also {
                    it.destroyRecursively()
                }
            }
            activeDevicesFLow.emit(scrcpyMap.keys.toSet())
        }
    }

    private fun withLock(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            isBusy.emit(true)
            mutex.withLock {
                try {
                    block()
                } catch (_: Throwable) {
                }
            }
        }.invokeOnCompletion {
            runBlocking(Dispatchers.IO) {
                isBusy.emit(false)
            }
        }
    }

    private fun Process.destroyRecursively() {
        descendants().forEach {
            //println("destroy child [${it.pid()}]")
            it.destroy()
        }
        //println("destroy [${pid()}]")
        destroy()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

}