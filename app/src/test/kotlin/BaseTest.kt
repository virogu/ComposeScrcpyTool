import com.virogu.core.PlateForm
import com.virogu.core.currentPlateForm
import com.virogu.core.init.InitTool
import com.virogu.core.init.InitToolDefault
import com.virogu.core.init.InitToolLinux
import com.virogu.core.init.InitToolWindows
import com.virogu.core.tool.ProgressTool
import com.virogu.core.tool.SSHTool
import com.virogu.core.tool.impl.ProgressToolsImpl
import com.virogu.core.tool.impl.SSHToolImpl
import com.virogu.core.tool.impl.SSHVerifyTools
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset


class BaseTest {
    private val sshTool: SSHTool = SSHToolImpl()
    private val progressTool: ProgressTool = ProgressToolsImpl()
    private val initTool: InitTool by lazy {
        when (currentPlateForm) {
            is PlateForm.Windows -> InitToolWindows()
            is PlateForm.Linux -> InitToolLinux(progressTool)
            else -> InitToolDefault()
        }
    }

    @Before
    fun prepare() {
        val resourcesDir = File(
            File("").absolutePath,
            "build/compose/tmp/prepareAppResources"
        ).absolutePath
        println("resources dir: $resourcesDir")
        System.setProperty("compose.application.resources.dir", resourcesDir)
        initTool.init()
    }

    @Test
    fun execTest(): Unit = runBlocking {
        sshTool.connect("192.168.5.134", SSHVerifyTools.user, SSHVerifyTools.pwd) {
            logger.info("ssh connected")
            exec(
                it,
                "setprop service.adb.tcp.port 5555",
                "stop adbd",
                "start adbd"
            ).onSuccess {
                logger.info("open device port success")
            }.onFailure { e ->
                logger.info("open device adb port fail:\n$e")
            }
        }.onFailure { e ->
            logger.info("ssh connect success fail:\n$e")
        }
    }

    @Test
    fun fileTest(): Unit = runBlocking {
        val serial = "192.168.5.134:5555"
        val path = "/storage/media/100/local/files/test"
        progressTool.exec(
            "hdc", "-t", serial, "shell",
            "ls", "-h", "-g", "-L", path,
            charset = Charset.forName("GBK"),
            consoleLog = true
        )
    }

    @Test
    fun fileDelTest(): Unit = runBlocking {
        val serial = "192.168.5.134:5555"
        val path = "/storage/media/100/local/files/test"
        progressTool.exec(
            "hdc", "-t", serial, "shell",
            "rm", "-h", "-g", "-L", path,
            charset = Charset.forName("GBK"),
            consoleLog = true
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BaseTest::class.java)
    }
}