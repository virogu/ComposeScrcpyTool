/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virogu.core.Common
import com.virogu.core.di.initDi
import com.virogu.core.tool.Tools
import com.virogu.core.viewmodel.ScrcpyViewModel
import com.virogu.ui.Pager
import com.virogu.ui.rememberPagerController
import io.github.oshai.kotlinlogging.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import theme.Icon
import tools.BuildConfig
import java.awt.SystemTray


//private val preferences = Preferences.userRoot()
//private const val KEY_LAST_WINDOWS_SIZE = "key-last-windows-size"

//val logger = KotlinLogging.logger("MainLogger")
val logger = KotlinLogging.logger("MainLogger")

private val tools by DI.global.instance<Tools>()

private val size by lazy {
//    val lastSize = preferences.get(KEY_LAST_WINDOWS_SIZE, "")
//    try {
//        if (lastSize.isNullOrEmpty()) {
//            throw IllegalArgumentException("lastSize is Null Or Empty")
//        }
//        val w = lastSize.split("*")[0].toFloat()
//        val h = lastSize.split("*")[1].toFloat()
//        DpSize(w.dp, h.dp)
//    } catch (e: Throwable) {
//        DpSize(700.dp, 620.dp)
//    }
    DpSize(700.dp, 820.dp)
}

fun main() {
    val platformInfo = """OS: [${Common.osName}, version:${Common.osVersion}]
        |Java Version: [${System.getProperty("java.version")}]
        |Java Path: [${System.getProperty("java.home")}]
    """.trimMargin()
    init()
    logger.info { "\n----Platform----\n$platformInfo\n--------" }
    startApplication()
}

private fun startApplication() = application {
    val icon = Icon.Logo
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = size,
        position = WindowPosition.Aligned(Alignment.Center),
    )
    val (alwaysOnTop, setAlwaysOnTop) = remember {
        mutableStateOf(false)
    }

    val pagerController by rememberPagerController(
        listOf(
            Pager.DeviceConnection,
            Pager.DeviceExplorer,
            Pager.DeviceProcess,
        ),
        Pager.DeviceConnection
    )
    val trayState = rememberTrayState()

    Window(
        onCloseRequest = ::exit,
        title = "${BuildConfig.AppName}_v${BuildConfig.BuildVersion}",
        state = state,
        undecorated = false,
        alwaysOnTop = alwaysOnTop,
        icon = icon,
    ) {
        App(window, this@application, state, trayState, pagerController, tools)
        if (SystemTray.isSupported()) {
            TrayView(trayState, icon, tools, state, alwaysOnTop) {
                setAlwaysOnTop(it)
            }
        }
    }
}

@Composable
private fun ApplicationScope.TrayView(
    trayState: TrayState,
    icon: Painter,
    tools: Tools,
    state: WindowState,
    alwaysOnTop: Boolean,
    scrcpyViewModel: ScrcpyViewModel = viewModel { ScrcpyViewModel() },
    onAlwaysOnTopChanged: (Boolean) -> Unit
) {
    val connectedDevice = tools.deviceConnect.connectedDevice.collectAsState()
    val currentDevice = tools.deviceConnect.currentSelectedDevice.collectAsState()
    val startedDevice = scrcpyViewModel.activeDevicesFLow.collectAsState()
    val connectedSize = remember(connectedDevice.value.size) {
        mutableStateOf(connectedDevice.value.size)
    }
    val startedSize = remember(startedDevice.value.size) {
        mutableStateOf(startedDevice.value.size)
    }

    val simpleConfigStore = tools.configStores.simpleConfigStore
    val onTopChanged by rememberUpdatedState(onAlwaysOnTopChanged)

    val simpleConfig = simpleConfigStore.simpleConfig.collectAsState()
    val autoRefresh by remember(simpleConfig.value.autoRefresh) {
        mutableStateOf(simpleConfig.value.autoRefresh)
    }
    val enableHdc by remember(simpleConfig.value.enableHdc) {
        mutableStateOf(simpleConfig.value.enableHdc)
    }
    Tray(
        icon = icon,
        state = trayState,
        tooltip = """ScrcpyTool
            |已连接设备：${connectedSize.value}
            |已启动设备：${startedSize.value}
            |自动刷新：${if (autoRefresh) "开" else "关"}
        """.trimMargin(),
        onAction = { state.isMinimized = !state.isMinimized },
        menu = {
            CheckboxItem("窗口置顶", alwaysOnTop) {
                onTopChanged(it)
            }
            Item(if (state.isMinimized) "显示主窗口" else "隐藏主窗口", onClick = {
                state.isMinimized = !state.isMinimized
            })
            Separator()
            Menu("设备列表") {
                if (connectedDevice.value.isEmpty()) {
                    Item("（空）") {}
                }
                connectedDevice.value.forEach { device ->
                    CheckboxItem(
                        device.showName,
                        device.serial == currentDevice.value?.serial
                    ) {
                        tools.deviceConnect.selectDevice(device)
                    }
                }
            }
            CheckboxItem("自动刷新", autoRefresh) {
                simpleConfigStore.updateSimpleConfig(simpleConfig.value.copy(autoRefresh = !autoRefresh))
            }
            CheckboxItem("启用hdc", enableHdc) {
                simpleConfigStore.updateSimpleConfig(simpleConfig.value.copy(enableHdc = !enableHdc))
            }
            Separator()
            Item("退出", onClick = ::exit)
        }
    )
}

private fun init() {
    logger.info { "init" }
    initDi()
    tools.start()
}

private fun ApplicationScope.exit() {
    logger.info { "exit app" }
    tools.stop()
    logger.info { "exited" }
    exitApplication()
}
