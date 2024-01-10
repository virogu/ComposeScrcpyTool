package com.virogu.core.tool.impl

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.DevicePlatform
import com.virogu.core.config.ConfigStores
import com.virogu.core.init.InitTool
import com.virogu.core.pingCommand
import com.virogu.core.tool.DeviceConnectTool
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.SSHTool
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.regex.Pattern

class DeviceConnectToolImpl(
    private val initTool: InitTool,
    private val configStores: ConfigStores,
    private val progressTool: ProgressTool,
) : BaseDeviceConnectTool() {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val devices: MutableStateFlow<List<DeviceInfo>> = MutableStateFlow(emptyList())

    private val autoRefresh = configStores.simpleConfigStore.simpleConfig.map {
        it.autoRefreshAdbDevice
    }.stateIn(scope, SharingStarted.Eagerly, true)

    override val connectedDevice: StateFlow<List<DeviceInfo>> = combine(
        configStores.deviceDescStore.deviceDescFlow,
        devices
    ) { deviceDesc, device ->
        device.map {
            it.copy(desc = deviceDesc[it.serial].orEmpty())
        }
    }.distinctUntilChanged().onEach { list ->
        val current = currentSelectedDevice.value
        if (current == null) {
            list.firstOrNull {
                it.isOnline
            }?.also {
                currentSelectedDevice.emit(it)
            }
        } else {
            //已选择的设备已经断开了的话自动选择第一个已连接的设备
            val find = list.find {
                it.serial == current.serial
            }
            if (find == null) {
                currentSelectedDevice.emit(list.firstOrNull { it.isOnline })
            } else if (find != current) {
                currentSelectedDevice.emit(find)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val currentSelectedDevice: MutableStateFlow<DeviceInfo?> = MutableStateFlow(null)

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val activeRefreshFlow = MutableSharedFlow<Any>()

    init {
        start()
    }

    private var mJob: Job? = null

    @OptIn(FlowPreview::class)
    override fun start() {
        mJob?.cancel()
        mJob = scope.launch {
            logger.info("等待程序初始化")
            initTool.initStateFlow.first {
                it.success
            }
            logger.info("初始化成功")
            activeRefreshFlow.debounce(5000).buffer(
                onBufferOverflow = BufferOverflow.DROP_LATEST
            ).onEach {
                autoRefresh()
            }.launchIn(this)
            delay(100)
            autoRefresh.onEach {
                activeRefresh()
            }.launchIn(this)
            autoRefresh()
        }
    }

    override fun selectDevice(device: DeviceInfo) {
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
            if (r.contains("cannot connect to", true) ||
                r.contains("unable to connect", true) ||
                r.contains("failed to connect", true) ||
                r.contains("connection refused", true)
            ) {
                logger.info("try open device adbd")
                openDeviceAdb(ip).onFailure { e ->
                    logger.info("ssh failed to connect [$ip]. $e")
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

    override fun disconnect(device: DeviceInfo) {
        withLock {
            val array = when (device.platform) {
                DevicePlatform.Android -> arrayOf("adb", "disconnect", device.serial)
            }
            progressTool.exec(*array, showLog = true)
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
            progressTool.exec("adb", "disconnect", showLog = true)
            innerRefreshDevices()
        }
    }

    private suspend fun activeRefresh() {
        activeRefreshFlow.emit(System.currentTimeMillis())
    }

    private suspend fun autoRefresh() {
        if (!autoRefresh.value) {
            return
        }
        if (!initTool.initStateFlow.value.success) {
            activeRefresh()
            return
        }
        mutex.withLock {
            innerRefreshDevices()
        }
    }

    private suspend fun innerRefreshDevices() {
        val list = mutableListOf<DeviceInfo>()
        list.addAll(refreshAndroidDevices())
        devices.emit(list)
        activeRefresh()
    }

    private suspend fun refreshAndroidDevices(): List<DeviceInfo> {
        val list = try {
            val process = progressTool.exec("adb", "devices", "-l").getOrThrow()
            val result = process.split("\n")
            result.mapNotNull { line ->
                //127.0.0.1:58526        device product:windows_x86_64 model:Subsystem_for_Android_TM_ device:windows_x86_64 transport_id:5
                val matcher = Pattern.compile(
                    "^(\\S+)\\s+(\\S+)\\s+product:(\\S+)\\s+model:(\\S+)\\s+device:(\\S+)\\s+(transport_id:)?(\\S+)?(.*)$"
                ).matcher(line.trim())
                if (!matcher.find()) {
                    return@mapNotNull null
                } else {
                    val serial = matcher.group(1) ?: return@mapNotNull null
                    val status = matcher.group(2) ?: return@mapNotNull null
                    val product = matcher.group(3) ?: return@mapNotNull null
                    val model = matcher.group(4) ?: return@mapNotNull null
                    val device = matcher.group(5) ?: return@mapNotNull null
                    val apiVersion = if (status == "device") {
                        adbGetProp(serial, "ro.build.version.sdk")
                    } else {
                        " Unknown"
                    }
                    val androidVersion = if (status == "device") {
                        adbGetProp(serial, "ro.build.version.release")
                    } else {
                        " Unknown"
                    }
                    DeviceInfo(
                        platform = DevicePlatform.Android,
                        serial = serial,
                        status = status,
                        product = product,
                        model = model,
                        version = androidVersion,
                        apiVersion = apiVersion,
                        device = device,
                    )
                }
            }.sortedByDescending {
                it.isOnline
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
        return list
    }

    override fun updateCurrentDesc(desc: String) {
        withLock {
            val current = currentSelectedDevice.value ?: return@withLock
            if (desc == current.desc) {
                return@withLock
            }
            val new = current.copy(desc = desc)
            configStores.deviceDescStore.updateDesc(current.serial, desc)
            currentSelectedDevice.emit(new)
        }
    }

    override fun stop() {
        mJob?.cancel()
        scope.cancel()
    }

    private suspend fun adbGetProp(serial: String, prop: String, default: String = ""): String {
        return progressTool.exec("adb", "-s", serial, "shell", "getprop", prop).getOrNull() ?: default
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
        private val logger: Logger = LoggerFactory.getLogger(DeviceConnectToolImpl::class.java)
    }
}

abstract class BaseDeviceConnectTool : DeviceConnectTool {

    private val sshTool: SSHTool by DI.global.instance()

    protected suspend fun openDeviceAdb(ip: String): Result<Boolean> {
        sshTool.connect(ip, SSHVerifyTools.user, SSHVerifyTools.pwd) {
            exec(
                it,
                "setprop service.adb.tcp.port 5555",
                "stop adbd",
                "start adbd"
            ).onSuccess {
                logger.info("open device adbd success")
            }.onFailure { e ->
                logger.info("open device adbd fail. $e")
            }
        }.fold({
            return Result.success(true)
        }, {
            return Result.failure(it)
        })
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BaseDeviceConnectTool::class.java)
    }

}