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
            "dumpsys activity processes"
        ).onSuccess {
            delay(10)
            //println(it)
            val process = it.parseToProcess()
            delay(10)
            processListFlow.emit(process)
        }
    }

    private fun String.parseToProcess(): List<ProcessInfo> {
        val matches = Regex("""(?s)(APP|PERS)\*\s+(.*?)\n\s*?[*\n]""").findAll(this)
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
                val user: String = baseInfo.groupValues.getOrNull(5).orEmpty()
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

    override fun killProcess(packageName: String) {
        withLock("kill $packageName") {
            val device = currentDevice ?: return@withLock
            progressTool.exec(
                "adb", "-s", device.serial, "shell", "am",
                "kill", packageName,
                consoleLog = true,
            ).onSuccess {
                if (it.isNotEmpty()) {
                    tipsFlow.emit(it)
                }
            }
            refreshProcess(device)
        }
    }

    override fun forceStopProcess(packageName: String) {
        withLock("force stop $packageName") {
            val device = currentDevice ?: return@withLock
            progressTool.exec(
                "adb", "-s", device.serial, "shell", "am",
                "force-stop", packageName,
                consoleLog = true,
            ).onSuccess {
                if (it.isNotEmpty()) {
                    tipsFlow.emit(it)
                }
            }
            refreshProcess(device)
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