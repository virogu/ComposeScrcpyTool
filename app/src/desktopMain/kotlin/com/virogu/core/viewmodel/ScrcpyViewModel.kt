package com.virogu.core.viewmodel

import androidx.lifecycle.ViewModel
import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.device.Device
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * @author Virogu
 * @since 2024-09-11 上午11:43
 **/
class ScrcpyViewModel : ViewModel() {
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val scrcpyMap = HashMap<String, Process>()

    val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val activeDevicesFLow = MutableStateFlow<Set<String>>(emptySet())

    fun connect(
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

    override fun onCleared() {
        disConnect()
        super.onCleared()
    }

    fun disConnect(device: Device? = null) {
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
            try {
                mutex.withLock {
                    isBusy.emit(true)
                    block()
                }
            } catch (_: Throwable) {
            } finally {
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
        private val logger = KotlinLogging.logger { }
    }

}