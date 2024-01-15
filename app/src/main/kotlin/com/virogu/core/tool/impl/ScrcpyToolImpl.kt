package com.virogu.core.tool.impl

import com.virogu.core.PlateForm
import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.commonWorkDir
import com.virogu.core.currentPlateForm
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.ScrcpyTool
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
        device: DeviceInfo,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ) {
        val serial = device.serial
        val title = device.showName
        withLock {
            if (scrcpyMap[serial] != null) {
                return@withLock
            }
            val progress = progressTool.execAsync(
                "scrcpy", "-s", serial, "--window-title=$title",
                *commonConfig.scrcpyArgs().toTypedArray(),
                *config.scrcpyArgs().toTypedArray(),
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

    override fun disConnect(device: DeviceInfo?) {
        withLock {
            if (device == null) {
                scrcpyMap.forEach { (_, progress) ->
                    progress.destroy()
                }
                scrcpyMap.clear()
            } else {
                if (!scrcpyMap.containsKey(device.serial)) {
                    return@withLock
                }
                scrcpyMap.remove(device.serial)?.also {
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