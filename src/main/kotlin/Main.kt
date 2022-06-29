import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.virogu.di.initDi
import com.virogu.tools.Tools
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory


//private val preferences = Preferences.userRoot()
//private const val KEY_LAST_WINDOWS_SIZE = "key-last-windows-size"

val logger: Logger = LoggerFactory.getLogger("MainLogger")

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
    DpSize(700.dp, 800.dp)
}

fun main() = application {
    init()
    val icon = painterResource("icon.ico")
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = size,
        position = WindowPosition.Aligned(Alignment.Center),
    )
    Window(
        onCloseRequest = ::exit,
        title = "工具",
        state = state,
        undecorated = false,
        icon = icon,
    ) {
        Tray(icon = icon, menu = {
            if (state.isMinimized) {
                Item("显示主窗口", onClick = {
                    state.isMinimized = false
                })
            } else {
                Item("隐藏主窗口", onClick = {
                    state.isMinimized = true
                })
            }
            Item("退出", onClick = ::exit)
        })
        App(window, this@application, state, tools)
    }
}

private fun init() {
    logger.info("init")
    initDi()
    val tools by DI.global.instance<Tools>()
    tools.logTool.start()
}

private fun ApplicationScope.exit() {
    logger.info("exit app")
    val tools by DI.global.instance<Tools>()
    tools.apply {
        sshTool.destroy()
        scrcpyTool.disConnect()
        progressTool.destroy()
        logTool.stop()
        configTool.writeConfigNow()
    }
    logger.info("exited")
    exitApplication()
}
