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

class InitToolMacOs : InitToolLinux() {
    private val cmd: BaseCommand by DI.global.instance<BaseCommand>()

    override suspend fun doInit() {
        super.doInit() // 执行 Linux 的复制和 chmod
        clearQuarantine() // 补充 Mac 特有的清理动作
    }

    private suspend fun clearQuarantine() {
        val xattrList = appList + listOf(
            File(workDir, "app/libusb_shared.dylib"),
            File(workDir, "app/scrcpy-server"),
        )
        xattrList.forEach { file ->
            // 清除隔离属性，防止系统弹窗拦截
            cmd.exec("xattr", "-d", "com.apple.quarantine", file.absolutePath).onFailure {
                // 忽略错误，因为如果文件本身没有隔离位，命令会报错
            }
        }
    }
}