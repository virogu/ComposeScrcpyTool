package com.virogu.tools.android

import com.virogu.tools.ProgressTool
import com.virogu.tools.appendCommonEnvironment
import com.virogu.tools.commonWorkDir
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


class ProgressToolsImpl : ProgressTool {

    private val processMapMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //private val runtime = Runtime.getRuntime()

    private val processList: HashMap<Long, Process> = HashMap()

    private val workDir: File by lazy {
        commonWorkDir
    }

    private val commandPath = mapOf(
        "adb" to File(workDir, "app/adb").absolutePath,
        "scrcpy" to File(workDir, "app/scrcpy").absolutePath,
    )

    override suspend fun exec(
        vararg command: String,
        environment: Map<String, String>,
        showLog: Boolean,
        consoleLog: Boolean,
        timeout: Long,
        charset: Charset
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (command.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("command is empty!"))
            }
            val cmd = command.toMutableList().apply {
                commandPath[first()]?.also {
                    this.removeFirst()
                    this.add(0, it)
                }
            }
            val process = ProcessBuilder(
                cmd,
            ).apply {
                directory(workDir)
                redirectErrorStream(true)
                val ev = environment()
                ev.apply {
                    appendCommonEnvironment()
                    putAll(environment)
                }
            }.start().also {
                it.onExit().thenApply {
                    scope.launch {
                        processMapMutex.withLock {
                            processList.remove(it.pid())
                        }
                    }
                }
                processMapMutex.withLock {
                    processList[it.pid()] = it
                }
            }
            if (showLog) {
                logger.info("$cmd wait")
            } else if (consoleLog) {
                println("$cmd wait")
            }
            val s = async {
                BufferedReader(InputStreamReader(process.inputStream, charset)).use {
                    buildString {
                        it.lineSequence().forEach { s ->
                            appendLine(s)
                        }
                    }.trim()
                }
            }
            if (timeout > 0) {
                process.waitFor(timeout, TimeUnit.SECONDS)
            } else {
                process.waitFor()
            }
            //val inputStreamReader = InputStreamReader(process.inputStream, charset)
            val result = s.await()
            if (showLog) {
                logger.info("$cmd result: [$result]")
            } else if (consoleLog) {
                println("$cmd result: [$result]")
            }
            return@withContext Result.success(result)
        } catch (e: Throwable) {
            //e.printStackTrace()
            if (e is CancellationException) {
                return@withContext Result.success("")
            }
            logger.error("run error. $e")
            return@withContext Result.failure(e)
        }
    }

    override suspend fun execAsync(
        vararg command: String,
        environment: Map<String, String>,
        charset: Charset,
        onReadLine: suspend (String) -> Unit,
    ): Process? = withContext(Dispatchers.IO) {
        try {
            if (command.isEmpty()) {
                return@withContext null
            }
            val cmd = command.toMutableList().apply {
                commandPath[first()]?.also {
                    this.removeFirst()
                    this.add(0, it)
                }
            }
            val processBuilder = ProcessBuilder(
                cmd,
            ).apply {
                directory(workDir)
                redirectErrorStream(true)
                val ev = environment()
                ev.apply {
                    appendCommonEnvironment()
                    putAll(environment)
                }
            }
            val progress = processBuilder.start()
            progress.onExit().thenApply {
                println("process ${it.pid()} exit[${it.exitValue()}]. ")
                scope.launch {
                    processMapMutex.withLock {
                        processList.remove(it.pid())
                    }
                }
            }
            processMapMutex.withLock {
                processList[progress.pid()] = progress
            }
            scope.launch {
                BufferedReader(progress.inputReader(charset)).use {
                    it.lineSequence().forEach { s ->
                        onReadLine(s)
                    }
                    println("read end")
                }
            }
            return@withContext progress
        } catch (e: Throwable) {
            logger.warn("执行失败, ${e.localizedMessage}")
            e.printStackTrace()
            return@withContext null
        }
    }

    override fun destroy() = runBlocking {
        processMapMutex.withLock {
            processList.onEach {
                runCatching {
                    it.value.destroy()
                }
                logger.info("stop process: $it")
            }
            processList.clear()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProgressToolsImpl::class.java)
    }

}