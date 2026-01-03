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

package com.virogu.core.command

import com.virogu.core.Common
import com.virogu.core.bean.Platform
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 6:23
 **/
class PingCommand : BaseCommand() {
    private val charset: Charset by lazy {
        when (Common.platform) {
            is Platform.Windows -> Charset.forName("GBK")
            else -> Charsets.UTF_8
        }
    }

    private val ping: String = "ping"

    private val pingArgs by lazy {
        when (Common.platform) {
            is Platform.Linux -> arrayOf("-c", "1")
            else -> arrayOf("-n", "1")
        }
    }

    suspend fun ping(ip: String): Boolean {
        val ping = exec(ping, ip, *pingArgs, outCharset = charset).getOrNull()
        return !(ping == null || !ping.contains("ttl=", true))
    }

}