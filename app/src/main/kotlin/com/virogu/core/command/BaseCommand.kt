package com.virogu.core.command

import com.virogu.core.isDebug
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:00
 **/
open class BaseCommand {

    protected open val workDir: File? = null
    private val processMap = HashMap<Long, Process>()
    protected var isActive = true
        private set
    private val mutex = Mutex()

    open suspend fun exec(
        vararg command: String,
        workDir: File? = this.workDir,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = isDebug,
        timeout: Long = 5L,
        charset: Charset = Charsets.UTF_8
    ): Result<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            var process: Process? = null
            try {
                if (command.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("command is empty!"))
                }
                val cmdString = command.joinToString(" ")
                if (showLog) {
                    logger.info("\n[${cmdString}] wait")
                } else if (consoleLog) {
                    logger.debug("\n[${cmdString}] wait")
                }
                process = ProcessBuilder(*command).fixEnv(env, workDir).start()
                val s = async {
                    process.inputReader(charset).use {
                        val builder = StringBuilder()
                        while (isActive) {
                            if (it.ready()) {
                                builder.appendLine(it.readText())
                                logger.debug("\n[${process.pid()}] [$cmdString] read finish")
                                break
                            }
                            if (!process.isAlive) {
                                if (it.ready()) {
                                    logger.debug("\n[${process.pid()}] [$cmdString] read line last")
                                    builder.appendLine(it.readText())
                                }
                                logger.debug("\n[${process.pid()}] [$cmdString] not alive")
                                break
                            }
                        }
                        builder.toString().trim()
                    }
                }
                //if (timeout <= 0) {
                //    process.waitFor()
                //} else {
                //    if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                //        logger.debug("\n[${process.pid()}] [$cmdString] time out after ${timeout}s")
                //        throw CancellationException("time out after ${timeout}s")
                //    }
                //}
                val result = s.await()
                if (showLog) {
                    val msg = "\n" + formatLog(cmdString, result)
                    logger.info(msg)
                } else if (consoleLog) {
                    val msg = "\n" + formatLog(cmdString, result)
                    logger.debug(msg)
                }
                return@withContext Result.success(result)
            } catch (e: Throwable) {
                e.printStackTrace()
                process?.destroyRecursively()
                if (e is CancellationException) {
                    logger.debug("job canceled, pid: ${process?.pid()}")
                    return@withContext Result.success("")
                }
                logger.error("run error. $e")
                return@withContext Result.failure(e)
            }
        }
    }

    open suspend fun execAsync(
        scope: CoroutineScope,
        vararg command: String,
        workDir: File? = this.workDir,
        env: Map<String, String>? = null,
        charset: Charset = Charsets.UTF_8,
        onReadLine: suspend (String) -> Unit,
    ): Process? = withContext(Dispatchers.IO) {
        var progress: Process? = null
        try {
            if (command.isEmpty()) {
                return@withContext null
            }
            val processBuilder = ProcessBuilder(*command).fixEnv(env, workDir)
            progress = processBuilder.start()
            scope.launch {
                progress.inputReader(charset).use {
                    it.lineSequence().forEach { s ->
                        println(s)
                        if (!isActive) {
                            progress?.destroy()
                            return@launch
                        }
                        onReadLine(s)
                    }
                    println("read end")
                }
            }
            return@withContext progress
        } catch (e: Throwable) {
            progress?.destroyRecursively()
            logger.warn("执行失败, ${e.localizedMessage}")
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun ProcessBuilder.fixEnv(
        extraEnv: Map<String, String>? = null,
        workDir: File? = null
    ): ProcessBuilder {
        directory(workDir)
        redirectErrorStream(true)
        extraEnv?.also {
            val ev = environment()
            ev.putAll(it)
        }
        return this
    }

    private fun formatLog(cmd: String, result: String): String {
        return """ ------
        | > $cmd
        | $result
        | ------ """.trimMargin()
    }

    //private fun addProcess(process: Process) {
    //    synchronized(processMap) {
    //        processMap[process.pid()]?.destroyRecursively()
    //        processMap[process.pid()] = process
    //    }
    //}

    //private fun removeProcess(pid: Long) {
    //    synchronized(processMap) {
    //        processMap.remove(pid)?.destroyRecursively()
    //    }
    //}

    private fun Process.destroyRecursively() {
        descendants().forEach {
            //println("destroy child [${it.pid()}]")
            it.destroy()
        }
        //println("destroy [${pid()}]")
        destroy()
    }

    fun destroy() {
        isActive = false
        runBlocking(Dispatchers.IO) {
            try {
                killServer()
            } catch (_: Throwable) {
            }
        }
        synchronized(processMap) {
            processMap.onEach {
                it.value.destroyRecursively()
            }
            processMap.clear()
        }
    }

    open suspend fun startServer() {

    }

    open suspend fun killServer() {

    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}