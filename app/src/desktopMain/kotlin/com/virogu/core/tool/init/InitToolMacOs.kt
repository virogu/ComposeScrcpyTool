/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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