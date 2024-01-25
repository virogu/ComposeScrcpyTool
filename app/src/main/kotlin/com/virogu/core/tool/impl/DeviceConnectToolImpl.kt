package com.virogu.core.tool.impl

import com.virogu.core.bean.DeviceInfo
import com.virogu.core.bean.DevicePlatform
import com.virogu.core.config.ConfigStores
import com.virogu.core.init.InitTool
import com.virogu.core.pingCommand
import com.virogu.core.tool.ProgressTool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.regex.Pattern

class DeviceConnectToolImpl(
    private val initTool: InitTool,
    private val configStores: ConfigStores,
    private val progressTool: ProgressTool,
) : DeviceConnectToolBase() {

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
    }.distinctUntilChanged().onEach(::onDeviceConnectedChanged).stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val currentSelectedDevice: MutableStateFlow<DeviceInfo?> = MutableStateFlow(null)

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        start()
    }

    private var mJob: Job? = null
    private var refreshJob: Job? = null

    override fun start() {
        mJob?.cancel()
        mJob = scope.launch {
            logger.info("等待程序初始化")
            initTool.initStateFlow.first {
                it.success
            }
            logger.info("初始化成功")
            delay(100)
            autoRefresh.onEach(::autoRefreshChanged).launchIn(this)
        }
    }

    private suspend fun onDeviceConnectedChanged(list: List<DeviceInfo>) {
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
    }

    private suspend fun autoRefreshChanged(enable: Boolean) {
        logger.info("auto refresh: $enable")
        refreshJob?.cancel()
        if (!enable) {
            return
        }
        refreshJob = scope.launch {
            while (isActive) {
                if (autoRefresh.value) {
                    autoRefresh()
                }
                delay(10_000)
            }
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
            var r = adbConnect(ip, port)
            if (!r) {
                r = hdcConnect(ip, port)
            }
            if (!r) {
                openDevicePort(ip, port).onSuccess {
                    logger.info("重新连接 [$ip:$port]")
                    r = adbConnect(ip, port)
                    if (!r) {
                        hdcConnect(ip, port)
                    }
                }
            }
            innerRefreshDevices()
        }
    }

    override fun disconnect(device: DeviceInfo) {
        withLock {
            val array = when (device.platform) {
                DevicePlatform.Android -> arrayOf("adb", "disconnect", device.serial)
                DevicePlatform.OpenHarmony -> arrayOf("hdc", "tconn", device.serial, "-remove")
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
            connectedDevice.value.filter {
                it.platform == DevicePlatform.OpenHarmony
            }.forEach {
                progressTool.exec("hdc", "tconn", it.serial, "-remove", showLog = true)
            }
            innerRefreshDevices()
        }
    }

    private suspend fun adbConnect(ip: String, port: Int): Boolean {
        progressTool.exec("adb", "disconnect", "${ip}:${port}")
        logger.info("try adb connect")
        val r = progressTool.exec("adb", "connect", "${ip}:${port}", timeout = 3).getOrNull()?.takeIf {
            it.isNotEmpty()
        } ?: run {
            return false
        }
        logger.info(r)
        // cannot connect to 192.168: 由于目标计算机积极拒绝，无法连接。 (10061)
        // 尝试通过SSH连接到设备再打开ADB
        val failed = r.contains("cannot connect to", true) ||
                r.contains("unable to connect", true) ||
                r.contains("failed to connect", true) ||
                r.contains("connection refused", true)
        return !failed
    }

    private suspend fun hdcConnect(ip: String, port: Int): Boolean {
        logger.info("try hdc connect")
        progressTool.exec("hdc", "tconn", "${ip}:${port}", "-remove", consoleLog = true)
        val r = progressTool.exec(
            "hdc", "tconn", "${ip}:${port}", timeout = 3, consoleLog = true
        ).getOrNull()?.takeIf {
            it.isNotEmpty()
        } ?: run {
            return false
        }
        logger.info(r)
        val failed = r.contains("failed", true)
        return !failed
    }

    private suspend fun autoRefresh() {
        if (!autoRefresh.value) {
            return
        }
        if (!initTool.initStateFlow.value.success) {
            return
        }
        innerRefreshDevices()
    }

    private suspend fun innerRefreshDevices() {
        val list = mutableListOf<DeviceInfo>()
        list.addAll(refreshAndroidDevices())
        list.addAll(refreshHarmonyDevices())
        devices.emit(list)
    }

    private suspend fun refreshAndroidDevices(): List<DeviceInfo> = try {
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
                val isOnline = status == "device"
                val apiVersion = if (isOnline) {
                    adbGetProp(serial, ANDROID_API_VERSION)
                } else {
                    " Unknown"
                }
                val androidVersion = if (isOnline) {
                    adbGetProp(serial, ANDROID_RELEASE_VERSION)
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
                    isOnline = isOnline
                )
            }
        }.sortedByDescending {
            it.isOnline
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        emptyList()
    }

    private suspend fun refreshHarmonyDevices(): List<DeviceInfo> = try {
        val process = progressTool.exec("hdc", "list", "targets", "-v", timeout = 2, consoleLog = false).getOrThrow()
        val result = process.split("\n")
        result.mapNotNull { line ->
            //192.168.5.128:10178   TCP     Offline                 hdc
            //192.168.5.128:5555    TCP     Offline     localhost   hdc
            //192.168.5.131:5555    TCP     Connected   localhost   hdc
            //192.168.5.255:5555    TCP     Offline                 hdc
            //COM1                  UART    Ready                   hdc
            val matcher = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+)(.*)$").matcher(line.trim())
            if (!matcher.find()) {
                return@mapNotNull null
            } else {
                val serial = matcher.group(1) ?: return@mapNotNull null
                //val type = matcher.group(2) ?: return@mapNotNull null
                val status = matcher.group(3) ?: return@mapNotNull null
                val isOnline = status.equals("Connected", ignoreCase = true)
                if (!isOnline) {
                    return@mapNotNull null
                }
                val apiVersion = hdcGetProp(serial, OHOS_API_VERSION)
                val releaseName = hdcGetProp(serial, OHOS_FULL_NAME)
                val product = hdcGetProp(serial, OHOS_PRODUCT_NAME)
                val model = hdcGetProp(serial, OHOS_MODEL_NAME)
                DeviceInfo(
                    platform = DevicePlatform.OpenHarmony,
                    serial = serial,
                    status = status,
                    product = product,
                    model = model,
                    apiVersion = apiVersion,
                    version = releaseName,
                    device = product,
                    isOnline = true,
                )
            }
        }.sortedByDescending {
            it.isOnline
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        emptyList()
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

    private suspend fun hdcGetProp(serial: String, prop: String, default: String = ""): String {
        delay(20)
        return progressTool.exec(
            "hdc", "-t", serial, "shell",
            "param", "get", prop, timeout = 1, consoleLog = false
        ).getOrNull()?.takeUnless {
            it.contains("Get", ignoreCase = true) && it.contains("fail", ignoreCase = true)
        } ?: default
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
        private const val ANDROID_API_VERSION = "ro.build.version.sdk"
        private const val ANDROID_RELEASE_VERSION = "ro.build.version.release"

        private const val OHOS_API_VERSION = "const.ohos.apiversion"
        private const val OHOS_FULL_NAME = "const.ohos.fullname"
        private const val OHOS_PRODUCT_NAME = "const.product.name"
        private const val OHOS_MODEL_NAME = "const.product.model"
    }
}
