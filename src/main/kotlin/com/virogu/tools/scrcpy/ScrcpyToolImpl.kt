package com.virogu.tools.scrcpy

import com.virogu.bean.ScrcpyConfig
import com.virogu.tools.PlateForm
import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.commonWorkDir
import com.virogu.tools.currentPlateForm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ScrcpyToolImpl(
    private val progressTool: ProgressTool
) : ScrcpyTool {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val scrcpyMap = HashMap<String, Process>()

    private val workDir: File by lazy {
        File(commonWorkDir, "app")
    }

    private val scrcpyEnvironment: Map<String, String> by lazy {
        when (currentPlateForm) {
            is PlateForm.Linux -> mapOf(
                "SCRCPY_ICON_PATH" to File(workDir, "logo.svg").absolutePath,
                "SCRCPY_SERVER_PATH" to File(workDir, "scrcpy-server").absolutePath,
                "ADB_PATH" to workDir.absolutePath,
            )

            else -> emptyMap()
        }
    }

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val activeDevicesFLow = MutableStateFlow<Set<String>>(emptySet())

    override fun connect(
        serial: String,
        title: String,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ) {
        withLock {
            if (scrcpyMap[serial] != null) {
                return@withLock
            }
            val progress = progressTool.execAsync(
                "scrcpy",
                "-s",
                serial,
                "--window-title=$title",
                *commonConfig.args().toTypedArray(),
                *config.args().toTypedArray(),
                environment = scrcpyEnvironment
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
                    it.destroyForcibly()
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
                } catch (_: Throwable) {
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