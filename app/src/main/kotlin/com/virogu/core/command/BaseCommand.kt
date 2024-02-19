package com.virogu.core.command

import com.virogu.core.isDebug
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:00
 **/
open class BaseCommand {

    protected open val workDir: File? = null
    private val processMap = HashMap<Long, Process>()

    open suspend fun exec(
        vararg command: String,
        workDir: File? = this.workDir,
        env: Map<String, String>? = null,
        showLog: Boolean = false,
        consoleLog: Boolean = isDebug,
        timeout: Long = 10L,
        charset: Charset = Charsets.UTF_8
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (command.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("command is empty!"))
            }
            val process = ProcessBuilder(*command).fixEnv(env, workDir).start().also { p ->
                addProcess(p)
                p.onExit().thenApply {
                    removeProcess(it.pid())
                }
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
            val cmdString = command.joinToString(" ")
            if (showLog) {
                logger.info("\n[${cmdString}] wait")
            } else if (consoleLog) {
                logger.debug("\n[${cmdString}] wait")
            }
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                logger.debug("\n[${process.pid()}] [$cmdString] time out after ${timeout}s")
                process.destroyRecursively()
            }
            //val inputStreamReader = InputStreamReader(process.inputStream, charset)
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
            //e.printStackTrace()
            if (e is CancellationException) {
                return@withContext Result.success("")
            }
            logger.error("run error. $e")
            return@withContext Result.failure(e)
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
        try {
            if (command.isEmpty()) {
                return@withContext null
            }
            val processBuilder = ProcessBuilder(*command).fixEnv(env, workDir)
            val progress = processBuilder.start().also { p ->
                addProcess(p)
                p.onExit().thenApply {
                    removeProcess(it.pid())
                }
            }
            scope.launch {
                BufferedReader(progress.inputReader(charset)).use {
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
        val f = File("")
        f.deleteRecursively()
        return this
    }

    private fun formatLog(cmd: String, result: String): String {
        return """ ------
        | > $cmd
        | $result
        | ------ """.trimMargin()
    }

    private fun addProcess(process: Process) {
        synchronized(processMap) {
            processMap[process.pid()]?.destroyRecursively()
            processMap[process.pid()] = process
        }
    }

    private fun removeProcess(pid: Long) {
        synchronized(processMap) {
            processMap.remove(pid)?.destroyRecursively()
        }
    }

    private fun Process.destroyRecursively() {
        descendants().forEach {
            //println("destroy child [${it.pid()}]")
            it.destroy()
        }
        //println("destroy [${pid()}]")
        destroy()
    }

    fun destroy() {
        synchronized(processMap) {
            processMap.onEach {
                it.value.destroyRecursively()
            }
            processMap.clear()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}