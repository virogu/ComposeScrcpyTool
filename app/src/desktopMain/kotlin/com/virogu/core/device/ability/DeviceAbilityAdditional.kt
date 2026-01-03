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

package com.virogu.core.device.ability

import com.virogu.core.bean.Additional
import com.virogu.core.config.ConfigStores
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import java.io.File

/**
 * @author Virogu
 * @since 2024-03-28 下午 12:21
 **/
abstract class DeviceAbilityAdditional {
    private val configStores by DI.global.instance<ConfigStores>()
    private val screenPath get() = configStores.scrcpyConfigStore.scrcpyConfigFlow.value.commonConfig.recordPath
    private val screenSaveDir get() = File(screenPath)

    abstract suspend fun exec(additional: Additional): String

    protected fun getScreenSavePath(): File {
        if (screenPath.isEmpty()) {
            throw IllegalArgumentException("请先设置[录像路径]")
        }
        val saveDir = screenSaveDir
        if (!saveDir.exists() || !saveDir.isDirectory) {
            throw IllegalArgumentException("[${saveDir.path}]不存在")
        }
        if (!saveDir.canWrite()) {
            throw IllegalArgumentException("[${saveDir.path}]不可写入")
        }
        return saveDir
    }
}