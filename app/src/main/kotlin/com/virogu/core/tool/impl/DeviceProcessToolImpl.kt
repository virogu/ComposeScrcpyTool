package com.virogu.core.tool.impl

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.DevicePlatform
import com.virogu.core.bean.ProcessInfo
import com.virogu.core.init.InitTool
import com.virogu.core.tool.DeviceConnectTool
import com.virogu.core.tool.DeviceProcessTool
import com.virogu.core.tool.ProgressTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
                delay(5000)
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

    private suspend fun refreshProcess(device: DeviceInfo) {
        when (device.platform) {
            DevicePlatform.Android -> adbRefreshProcess(device.serial)
        }
    }

    private suspend fun adbRefreshProcess(serial: String) {
        progressTool.exec(
            "adb", "-s", serial, "shell", "dumpsys activity processes"
        ).onSuccess {
            delay(10)
            //println(it)
            val process = it.androidParseProcess()
            delay(10)
            processListFlow.emit(process)
        }
    }

    private fun String.androidParseProcess(): List<ProcessInfo> {
        val matches = Regex("""(?s)(APP|PERS)\*\s+(.*?)(\*|PID mappings:)""").findAll(this)
        if (matches.count() <= 0) {
            return emptyList()
        }
        return try {
            matches.mapNotNull { matchesResult ->
                val process = matchesResult.groupValues[2]
                val first = process.reader().readLines().firstOrNull()?.trim() ?: return@mapNotNull null
                //UID 1000 ProcessRecord{a0f6d00 727:com.microsoft.windows.systemapp/u0a48}
                val baseInfo = Regex("""\S+\s+(\d+)\s+ProcessRecord\{(\S+)\s+(\d+):(\S+)/(\S+)}(.*)""").find(first)
                    ?: return@mapNotNull null
                val uid = baseInfo.groupValues.getOrNull(1) ?: return@mapNotNull null
                val pid = baseInfo.groupValues.getOrNull(3) ?: return@mapNotNull null
                val processName = baseInfo.groupValues.getOrNull(4) ?: return@mapNotNull null
                val packageName = processName.split(":").firstOrNull() ?: return@mapNotNull null
                val user: String = baseInfo.groupValues.getOrNull(5).orEmpty().let {
                    if (it.first().isDigit()) {
                        it.toIntOrNull()?.toString() ?: "0"
                    } else {
                        val m = Regex("""(.*?)(\d+)(.*?)""").find(it)
                        m?.groupValues?.getOrNull(2) ?: "0"
                    }
                }
                val maps = Regex("""\s*(\w+)=(\{[^{}]*}|\S+)\s*""").findAll(process).associate { kv ->
                    kv.groupValues[1] to kv.groupValues[2]
                }
                ProcessInfo(
                    user = user,
                    uid = uid,
                    pid = pid,
                    processName = processName,
                    packageName = packageName,
                    params = maps
                )
            }.toList()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun killProcess(info: ProcessInfo) {
        withLock("kill ${info.pid}") {
            val device = currentDevice ?: return@withLock
            val array = when (device.platform) {
                DevicePlatform.Android -> arrayOf("adb", "-s", device.serial, "shell", "am", "kill", info.packageName)
            }
            progressTool.exec(*array, consoleLog = true).toast()
            refreshProcess(device)
        }
    }

    override fun forceStopProcess(info: ProcessInfo) {
        withLock("force stop ${info.packageName}") {
            val device = currentDevice ?: return@withLock
            val array = when (device.platform) {
                DevicePlatform.Android -> arrayOf(
                    "adb", "-s", device.serial,
                    "shell", "am", "force-stop", info.packageName,
                )
            }
            progressTool.exec(*array, consoleLog = true).toast()
            refreshProcess(device)
        }
    }

    private suspend fun Result<String>.toast() = onSuccess {
        if (it.isNotEmpty()) {
            tipsFlow.emit(it)
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