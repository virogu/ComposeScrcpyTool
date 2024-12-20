/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.device.ability.adb

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.command.BaseCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityScrcpy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class AndroidDeviceScrcpyAbility(
    private val device: Device
) : DeviceAbilityScrcpy {
    companion object {
        private val cmd: BaseCommand by DI.global.instance<BaseCommand>()
        private val logger = KotlinLogging.logger { }
    }

    private val target = arrayOf("-s", device.serial)

    private val workDir: File by lazy {
        Common.workDir.resolve("app")
    }

    private val executable by lazy {
        when (Common.platform) {
            is Platform.Linux, is Platform.MacOs -> arrayOf("./scrcpy")
            is Platform.Windows -> arrayOf("cmd.exe", "/c", "scrcpy")
            else -> arrayOf("scrcpy")
        }
    }

    private val environment: Map<String, String> by lazy {
        when (Common.platform) {
            is Platform.Linux, is Platform.MacOs -> mapOf(
                "SCRCPY_ICON_PATH" to File(workDir, "logo.svg").absolutePath,
                "SCRCPY_SERVER_PATH" to File(workDir, "scrcpy-server").absolutePath,
                "ADB_PATH" to workDir.absolutePath,
            )

            else -> emptyMap()
        }
    }

    override suspend fun connect(
        scope: CoroutineScope,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ): Process? {
        val title = device.showName
        val command = arrayOf(
            *executable, *target, "--window-title=$title",
            *commonConfig.scrcpyArgs().toTypedArray(),
            *config.scrcpyArgs().toTypedArray(),
        )
        return cmd.execAsync(
            scope,
            *command,
            workDir = workDir,
            env = environment
        ) {
            logger.info { it }
        }
    }
}