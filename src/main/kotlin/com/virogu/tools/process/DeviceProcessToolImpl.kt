package com.virogu.tools.process

import com.virogu.bean.AdbDevice
import com.virogu.bean.ProcessInfo
import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.connect.DeviceConnectTool
import com.virogu.tools.init.InitTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.regex.Pattern

class DeviceProcessToolImpl(
    private val initTool: InitTool,
    deviceConnectTool: DeviceConnectTool,
    private val progressTool: ProgressTool,
) : DeviceProcessTool {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val jobsMutex = Mutex()
    private val loadingJobs = mutableMapOf<String, Job>()
    private var mJob: Job? = null

    @Volatile
    private var active = false

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val selectedOnlineDevice = deviceConnectTool.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    override val processListFlow: MutableStateFlow<List<ProcessInfo>> = MutableStateFlow(emptyList())

    override val tipsFlow = MutableSharedFlow<String>()

    init {
        start()
    }

    private fun start() {
        scope.launch {
            initTool.initStateFlow.first {
                it.success
            }
            selectedOnlineDevice.onEach {
                initJob()
            }.launchIn(scope)
        }
    }

    private fun initJob() {
        mJob?.cancel()
        mJob = scope.launch {
            processListFlow.emit(emptyList())
            if (!active) {
                return@launch
            }
            while (isActive) {
                val device = currentDevice
                if (active && device != null) {
                    refreshProcess(device)
                }
                delay(3000)
            }
        }
    }

    override fun pause() {
        active = false
        initJob()
    }

    override fun active() {
        active = true
        initJob()
    }

    override fun refresh() {
        withLock("refresh") {
            val device = currentDevice ?: return@withLock
            refreshProcess(device)
        }
    }

    private suspend fun refreshProcess(device: AdbDevice) {
        progressTool.exec(
            "adb", "-s", device.serial, "shell",
            "dumpsys", "cpuinfo",
        ).onSuccess {
            delay(10)
            //println(it)
            val lines = it.trim().split("\n")
            val process = lines.parseToProcess()
            delay(10)
            processListFlow.emit(process)
        }
    }

    private fun List<String>.parseToProcess(): List<ProcessInfo> {
        if (this.isEmpty()) {
            return emptyList()
        }
        return try {
            mapNotNull { line ->
                val matcher = Pattern.compile(
                    "^(\\S+)\\s+([^/]+)/(\\S+):\\s+(.*)$"
                ).matcher(line.trim())
                if (matcher.find()) {
                    ProcessInfo(
                        cpuRate = matcher.group(1),
                        pid = matcher.group(2),
                        name = matcher.group(3)
                    )
                } else {
                    null
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun killProcess(pid: String) {
        withLock("kill $pid") {
            val device = currentDevice ?: return@withLock
            //TODO()
        }
    }

    override fun forceStopProcess(pid: String) {
        withLock("force stop $pid") {
            val device = currentDevice ?: return@withLock
            //TODO()
        }
    }

    private fun withLock(tag: String, block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            mutex.withLock {
                try {
                    block()
                } catch (_: Throwable) {
                } finally {
                }
            }
        }.also { job ->
            jobLock {
                it[tag]?.cancel()
                it[tag] = job
            }
            job.invokeOnCompletion {
                jobLock {
                    it.remove(tag)
                }
            }
        }
    }

    private fun <T> jobLock(block: suspend CoroutineScope.(MutableMap<String, Job>) -> T) = runBlocking {
        jobsMutex.withLock {
            block(loadingJobs).also {
                isBusy.emit(mutex.isLocked)
            }
        }
    }
}