package com.virogu.tools.init

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.commonWorkDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class LinuxInitTool(
    private val progressTool: ProgressTool
) : InitTool {

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState(true))

    override fun init() {
        runBlocking {
            withContext(Dispatchers.IO) {
                innerInit()
            }
        }
    }

    private suspend fun innerInit() = runCatching {
        val cannotRunFiles = mutableListOf<File>()
        val cannotWriteFiles = mutableListOf<File>()
        listOf(
            File(commonWorkDir, "app/adb"),
            File(commonWorkDir, "app/scrcpy"),
            File(commonWorkDir, "app/scrcpy-main"),
        ).forEach { f ->
            val runnable = progressTool.exec(
                "sh", "-c", "test -x '${f.absolutePath}' && echo '1' || echo '0'",
            ).fold({
                println("test -x ${f.absolutePath}, result: [$it]")
                it.trim() == "1"
            }, {
                println("[$it]")
                false
            })
            if (!runnable) {
                println("${f.absolutePath} can not run")
                cannotRunFiles.add(f)
            }
        }
        listOf(
            File("app"),
        ).forEach { f ->
            if (!f.canWrite() || !f.canRead()) {
                cannotWriteFiles.add(f.absoluteFile)
            }
        }
        if (cannotRunFiles.isEmpty() && cannotWriteFiles.isEmpty()) {
            initStateFlow.emit(InitState(true))
        } else {
            val msg = buildString {
                appendLine("程序相关文件无法执行/写入，请复制以下命令到终端里面执行，然后再重启程序")
            }
            val subMsg = buildString {
                cannotRunFiles.forEach { f ->
                    appendLine("sudo chmod +x ${f.absolutePath}")
                }
                cannotWriteFiles.forEach { f ->
                    appendLine("sudo mkdir ${f.absolutePath}")
                    appendLine("sudo chmod -R a+rwX ${f.absolutePath}")
                }
            }
            initStateFlow.emit(InitState(false, msg, subMsg))
        }
    }

}