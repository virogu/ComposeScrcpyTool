package com.virogu.core.tool

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