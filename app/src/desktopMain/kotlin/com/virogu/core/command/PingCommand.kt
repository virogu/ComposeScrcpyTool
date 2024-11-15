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