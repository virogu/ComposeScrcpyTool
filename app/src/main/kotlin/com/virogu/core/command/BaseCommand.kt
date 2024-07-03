package com.virogu.core.command

import com.virogu.core.isDebug
import com.virogu.core.projectTmpDir
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * @author Virogu
 * @since 2024-03-27 下午 5:00
 **/
open class BaseCommand {

    protected open val workDir: File? = null
    private val processMap = HashMap<Long, Process>()
    protected var active = true
        private set
    private val mutex = Mutex()

    private val tmpDir by lazy {
        projectTmpDir
    }

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
            val random = Random.nextLong(100000, 999999)
            val redirectFile = File(tmpDir, "${System.currentTimeMillis()}_${random}")
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
                // 将执行结果重定向到一个临时文件，执行结束后读取这个文件的内容，再把文件删除
                // 因为执行hdc命令时，inputStream read时总是会卡死
                // 这样操作虽然仍无法避免hdc inputStream卡死，但是卡死时不会影响正常运行，只是这个临时文件被hdc进程锁死无法删除
                // 程序正常关闭时会kill掉hdc服务，从而释放相关被锁定的文件，下次程序启动会自动清空临时目录
                process = ProcessBuilder(*command).fixEnv(env, workDir, redirectFile).start()
                if (timeout <= 0) {
                    process.waitFor()
                } else {
                    if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                        logger.debug("\n[${process.pid()}] [$cmdString] time out after ${timeout}s")
                        throw CancellationException("time out after ${timeout}s")
                    }
                }
                val result = redirectFile.readText(charset).trim()
                if (showLog) {
                    val msg = "\n" + formatLog(cmdString, result)
                    logger.info(msg)
                } else if (consoleLog) {
                    val msg = "\n" + formatLog(cmdString, result)
                    logger.debug(msg)
                }
                return@withContext Result.success(result)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    logger.debug("job canceled, pid: ${process?.pid()}")
                    return@withContext Result.success("")
                }
                e.printStackTrace()
                logger.error("run error. $e")
                return@withContext Result.failure(e)
            } finally {
                process?.destroyRecursively()
                redirectFile.takeIf {
                    it.exists()
                }?.delete()?.also {
                    if (!it) {
                        logger.debug("delete tmp file ${redirectFile.name} fail")
                    }
                }
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
        workDir: File? = null,
        redirectFile: File? = null
    ): ProcessBuilder {
        directory(workDir)
        if (redirectFile != null) {
            redirectOutput(ProcessBuilder.Redirect.INHERIT)

            redirectOutput(redirectFile)
            redirectError(redirectFile)
        } else {
            redirectErrorStream(true)
        }
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

    private fun Process.destroyRecursively() {
        descendants().forEach {
            //println("destroy child [${it.pid()}]")
            it.destroy()
        }
        //println("destroy [${pid()}]")
        destroy()
    }

    fun destroy() {
        active = false
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