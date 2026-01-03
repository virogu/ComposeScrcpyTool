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

package com.virogu.core.tool.ssh

import org.apache.sshd.client.session.ClientSession
import java.nio.charset.Charset

interface SSHTool {

    suspend fun connect(
        host: String,
        user: String,
        password: String,
        port: Int = 22,
        timeout: Long = 30_000L,
        doOnConnected: suspend SSHTool.(ClientSession) -> Unit = {}
    ): Result<Unit>

    suspend fun exec(
        session: ClientSession,
        vararg cmds: String,
        charset: Charset = Charsets.UTF_8
    ): Result<String>

    fun destroy()

}