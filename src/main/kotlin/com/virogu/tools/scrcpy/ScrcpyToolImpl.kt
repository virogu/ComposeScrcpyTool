package com.virogu.tools.scrcpy

import com.virogu.bean.Configs
import com.virogu.tools.adb.ProgressTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ScrcpyToolImpl(
    private val progressTool: ProgressTool
) : ScrcpyTool {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val scrcpyMap = HashMap<String, Process>()

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val activeDevicesFLow: MutableStateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet())

    override fun connect(serial: String, config: Configs.ScrcpyConfig) {
        withLock {
            if (scrcpyMap[serial] != null) {
                return@withLock
            }
            val progress = progressTool.execAsync(
                "scrcpy",
                "-s",
                serial,
            ) {
                logger.info(it)
            } ?: run {
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

    override fun disConnect(serial: String?) {
        withLock {
            if (serial == null) {
                scrcpyMap.forEach { (_, progress) ->
                    progress.destroy()
                }
                scrcpyMap.clear()
            } else {
                if (!scrcpyMap.containsKey(serial)) {
                    return@withLock
                }
                scrcpyMap.remove(serial)?.also {
                    it.destroy()
                }
            }
            activeDevicesFLow.emit(scrcpyMap.keys.toSet())
        }
    }

    private fun withLock(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            mutex.withLock {
                isBusy.emit(true)
                try {
                    block()
                } finally {
                    isBusy.emit(false)
                }
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ScrcpyToolImpl::class.java)
    }

}