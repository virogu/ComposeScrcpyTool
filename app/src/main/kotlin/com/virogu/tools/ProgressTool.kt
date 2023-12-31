package com.virogu.tools

import java.nio.charset.Charset

interface ProgressTool {

    suspend fun exec(
        vararg command: String,
        environment: Map<String, String> = emptyMap(),
        showLog: Boolean = false,
        consoleLog: Boolean = false,
        timeout: Long = 10L,
        charset: Charset = Charsets.UTF_8
    ): Result<String>

    suspend fun execAsync(
        vararg command: String,
        environment: Map<String, String> = emptyMap(),
        charset: Charset = Charsets.UTF_8,
        onReadLine: suspend (String) -> Unit
    ): Process?

    //suspend fun adbExec(command: String, showLog: Boolean = false, onCmdString: (String) -> Unit = {}): Result<String>

    fun destroy()

}