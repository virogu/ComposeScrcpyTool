package com.virogu.tools.adb

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


class ProgressToolsImpl : ProgressTool {

    private val processMapMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    //private val runtime = Runtime.getRuntime()

    private val processList: HashMap<Long, Process> = HashMap()

    private val workDir: File by lazy {
        val file = File(System.getProperty("compose.application.resources.dir")).also {
            logger.info("resourcesDir: ${it.absolutePath}")
        }
        //logger.info("workFile: ${file.absolutePath}")
        file.absoluteFile
    }

    private val env = mapOf(
        "LANG" to "en_US.UTF-8",
    )

    private val commandPath = mapOf(
        "adb" to File(workDir, "app/adb").absolutePath,
        "scrcpy" to File(workDir, "app/scrcpy").absolutePath,
    )

    override suspend fun exec(
        vararg command: String,
        environment: Map<String, String>,
        showLog: Boolean,
        timeout: Long,
        charset: Charset
    ): Result<String> {
        try {
            if (command.isEmpty()) {
                return Result.failure(IllegalArgumentException("command is empty!"))
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
                    putAll(env)
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
                logger.info("exec [$cmd], waitFor result")
            }
            if (timeout > 0) {
                process.waitFor(timeout, TimeUnit.SECONDS)
            } else {
                process.waitFor()
            }
            //val inputStreamReader = InputStreamReader(process.inputStream, charset)
            val result = process.inputReader(charset).readText().trim()
            if (showLog) {
                logger.info("exec [$cmd], result: [$result]")
            }
            return Result.success(result)
        } catch (e: Throwable) {
            //e.printStackTrace()
            logger.error("run error. $e")
            return Result.failure(e)
        }
    }

    override suspend fun execAsync(
        vararg command: String,
        environment: Map<String, String>,
        charset: Charset,
        onReadLine: suspend (String) -> Unit,
    ): Process? {
        try {
            if (command.isEmpty()) {
                return null
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
                    putAll(env)
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
                    runCatching {
                        val first = it.readLine()
                        if (first.isNotEmpty()) {
                            onReadLine(first)
                        }
                    }
                    while (progress.isAlive && isActive) {
                        if (it.ready()) {
                            val s = it.readLine()
                            onReadLine(s)
                        }
                        delay(100)
                    }
                }
            }
            return progress
        } catch (e: Throwable) {
            logger.warn("执行失败, ${e.localizedMessage}")
            e.printStackTrace()
            return null
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