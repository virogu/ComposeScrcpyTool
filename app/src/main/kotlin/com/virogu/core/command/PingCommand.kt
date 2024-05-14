package com.virogu.core.command

import com.virogu.core.PlateForm
import com.virogu.core.currentPlateForm
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 6:23
 **/
class PingCommand : BaseCommand() {
    private val mutex = Mutex()

    private val charset: Charset by lazy {
        when (currentPlateForm) {
            is PlateForm.Windows -> Charset.forName("GBK")
            else -> Charsets.UTF_8
        }
    }

    private val ping: String = "ping"

    private val pingArgs by lazy {
        when (currentPlateForm) {
            is PlateForm.Linux -> arrayOf("-c", "1")
            else -> arrayOf("-n", "1")
        }
    }

    suspend fun ping(ip: String): Boolean = mutex.withLock {
        val ping = exec(ping, ip, *pingArgs, charset = charset).getOrNull()
        return !(ping == null || !ping.contains("ttl=", true))
    }

}