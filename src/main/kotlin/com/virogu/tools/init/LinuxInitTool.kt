package com.virogu.tools.init

import com.virogu.tools.adb.ProgressTool
import com.virogu.tools.commonResourceDir
import com.virogu.tools.commonWorkDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class LinuxInitTool(
    private val progressTool: ProgressTool
) : InitTool {

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState(false))

    override fun init() {
        runBlocking {
            withContext(Dispatchers.IO) {
                innerInit()
            }
        }
    }

    private suspend fun innerInit() = runCatching {
        File(commonResourceDir, "app").listFiles()?.forEach {
            val f = File(commonWorkDir, "app/${it.name}")
            if (!f.exists()) {
                it.copyTo(f, true)
                println("copy [$it] to [$f]")
            }
        }
        File(commonResourceDir, "files").listFiles()?.forEach {
            val f = File(commonWorkDir, "files/${it.name}")
            if (!f.exists()) {
                it.copyTo(f, true)
                println("copy [$it] to [$f]")
            }
        }
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
            if (runnable) {
                return@forEach
            }
            progressTool.exec(
                "chmod", "+x", f.absolutePath
            ).onSuccess {
                println("chmod +x ${f.absolutePath}, result: [$it]")
            }.onFailure {
                println("chmod +x ${f.absolutePath}, result: [$it]")
            }
        }
        initStateFlow.emit(InitState(true))
    }

}