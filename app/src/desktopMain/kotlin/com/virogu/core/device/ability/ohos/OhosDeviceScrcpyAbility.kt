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

package com.virogu.core.device.ability.ohos

import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.command.HdcCommand
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityScrcpy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:47
 **/
class OhosDeviceScrcpyAbility(device: Device) : DeviceAbilityScrcpy {
    companion object {
        private val cmd: HdcCommand by DI.global.instance<HdcCommand>()
        private val logger = KotlinLogging.logger { }
    }

    override suspend fun connect(
        scope: CoroutineScope,
        commonConfig: ScrcpyConfig.CommonConfig,
        config: ScrcpyConfig.Config
    ): Process? {
        logger.warn { "当前设备暂不支持" }
        return null
    }
}