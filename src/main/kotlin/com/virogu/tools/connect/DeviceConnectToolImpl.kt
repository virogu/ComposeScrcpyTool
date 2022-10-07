package com.virogu.tools.connect

import com.virogu.bean.AdbDevice
import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.config.ConfigTool
import com.virogu.tools.init.InitTool
import com.virogu.tools.pingCommand
import com.virogu.tools.sshd.SSHTool
import com.virogu.tools.sshd.SSHVerifyTools
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset

class DeviceConnectToolImpl(
    private val initTool: InitTool,
    private val configTool: ConfigTool,
    private val progressTool: ProgressTool,
    private val sshTool: SSHTool,
) : DeviceConnectTool {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val devices: MutableStateFlow<List<AdbDevice>> = MutableStateFlow(emptyList())

    override val connectedDevice: StateFlow<List<AdbDevice>> = combine(
        configTool.deviceDescFlow,
        devices
    ) { deviceDesc, device ->
        device.map {
            it.copy(
                desc = deviceDesc[it.serial].orEmpty().ifEmpty { "Phone" }
            )
        }
    }.distinctUntilChanged().onEach { list ->
        if (currentSelectedDevice.value == null) {
            list.firstOrNull()?.also {
                currentSelectedDevice.emit(it)
            }
        } else {
            currentSelectedDevice.value?.serial?.also { name ->
                val needClean = list.find {
                    it.serial == name
                } == null
                if (needClean) {
                    currentSelectedDevice.emit(list.firstOrNull())
                }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val currentSelectedDevice: MutableStateFlow<AdbDevice?> = MutableStateFlow(null)

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var queryJob: Job? = null
    private val jobActiveFlow = MutableSharedFlow<Any>()

    init {
        start()
    }

    @OptIn(FlowPreview::class)
    override fun start() {
        queryJob?.cancel()
        queryJob = jobActiveFlow.debounce(5000).buffer(
            onBufferOverflow = BufferOverflow.DROP_LATEST
        ).onEach {
            if (initTool.initStateFlow.value.success) {
                autoRefresh()
            } else {
                jobActiveFlow.emit(System.currentTimeMillis())
            }
        }.launchIn(scope)
        scope.launch {
            logger.info("等待程序初始化")
            initTool.initStateFlow.first {
                it.success
            }
            logger.info("初始化成功")
            delay(500)
            autoRefresh()
        }
    }

    override fun selectDevice(device: AdbDevice) {
        currentSelectedDevice.tryEmit(device)
    }

    override fun connect(ip: String, port: Int) {
        withLock {
            logger.info("正在连接 [$ip:$port]")
            pingCommand?.also {
                val ping = progressTool.exec(
                    *it, ip,
                    charset = Charset.forName("GBK")
                ).getOrNull()
                //logger.info(ping)
                if (ping == null || !ping.contains("ttl=", true)) {
                    logger.warn("无法访问 [$ip], 请检查设备是否在线")
                    return@withLock
                }
            }
            val r = connectTo(ip, port).getOrNull().orEmpty()
            logger.info(r)
            // cannot connect to 192.168: 由于目标计算机积极拒绝，无法连接。 (10061)
            // 尝试通过SSH连接到设备再打开ADB
            if (r.contains("cannot connect to")) {
                logger.info("try open device adbd")
                sshTool.connect(ip, "root", SSHVerifyTools.commonSShPwd) {
                    exec(
                        it,
                        "setprop service.adb.tcp.port 5555",
                        "stop adbd",
                        "start adbd"
                    ).onSuccess {
                        logger.info("open device adbd success")
                    }.onFailure { e ->
                        logger.info("open device adbd fail.", e)
                    }
                }.onFailure { e ->
                    logger.info("ssh failed to connect [$ip].", e)
                }
                logger.info("重新连接 [$ip:$port]")
                connectTo(ip, port).getOrNull()?.also {
                    logger.info(it)
                }
            }
            innerRefreshDevices()
        }
    }

    private suspend fun connectTo(ip: String, port: Int): Result<String> {
        progressTool.exec("adb", "disconnect", "${ip}:${port}")
        return progressTool.exec("adb", "connect", "${ip}:${port}")
    }

    override fun disconnect(device: AdbDevice) {
        withLock {
            progressTool.exec("adb", "disconnect", device.serial).getOrNull()?.also {
                logger.info(it)
            }
            innerRefreshDevices()
        }
    }

    override fun refresh() {
        withLock {
            innerRefreshDevices()
        }
    }

    override fun disconnectAll() {
        withLock {
            progressTool.exec("adb", "disconnect").getOrNull()?.also {
                logger.info(it)
            }
            innerRefreshDevices()
        }
    }

    private suspend fun autoRefresh() {
        mutex.withLock {
            innerRefreshDevices()
        }
    }

    private suspend fun innerRefreshDevices() {
        val list = mutableListOf<AdbDevice>()
        try {
            val process = progressTool.exec("adb", "devices", showLog = false).getOrThrow()
            val result = process.split("\n")
            if (result.size >= 2) {
                //("未获取到任何设备，请检查设备连接")
                for (i in 1 until result.size) {
                    val item = result[i].trim()
                    if (!item.endsWith("device")) {
                        continue
                    }
                    val device = item.split("device")
                    if (device.isEmpty()) {
                        continue
                    }
                    val deviceId = device[0].trim()
                    if (deviceId.isNotEmpty()) {
                        list.add(AdbDevice(serial = deviceId))
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        devices.emit(list)
        jobActiveFlow.emit(System.currentTimeMillis())
    }

    override fun updateCurrentDesc(desc: String) {
        withLock {
            val current = currentSelectedDevice.value ?: return@withLock
            if (desc == current.desc) {
                return@withLock
            }
            val new = current.copy(
                desc = desc.ifEmpty { "Phone" }
            )
            configTool.updateDesc(current.serial, desc)
            currentSelectedDevice.emit(new)
        }
    }

    override fun stop() {
        queryJob?.apply {
            cancel()
            queryJob = null
        }
        scope.cancel()
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
        private val logger: Logger = LoggerFactory.getLogger(DeviceConnectToolImpl::class.java)
    }
}