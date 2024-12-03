package com.virogu.core.tool.init

import com.virogu.core.command.BaseCommand
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File

class InitToolMacOs : InitToolDefault() {
    private val cmd: BaseCommand by DI.global.instance<BaseCommand>()
    override suspend fun doInit() {
        innerInit()
    }

    private suspend fun innerInit() = runCatching {
        listOf(
            File(workDir, "app/adb"),
            File(workDir, "app/scrcpy"),
            File(workDir, "app/scrcpy_bin"),
            File(workDir, "app/hdc"),
        ).forEach { f ->
            chmodX(f.absolutePath)
        }
    }

    private suspend fun chmodX(absolutePath: String) = runCatching {
        cmd.exec("sh", "-c", "chmod +x '${absolutePath}'").onSuccess {
            println("chmod +x ${absolutePath}, success: [$it]")
        }.onFailure {
            println("chmod +x ${absolutePath}, fail: [$it]")
        }
    }
}