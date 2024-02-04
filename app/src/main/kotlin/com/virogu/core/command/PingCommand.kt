package com.virogu.core.command

import com.virogu.core.PlateForm
import com.virogu.core.currentPlateForm
import java.nio.charset.Charset

/**
 * @author Virogu
 * @since 2024-03-27 下午 6:23
 **/
class PingCommand : BaseCommand() {
    private val charset: Charset by lazy {
        when (currentPlateForm) {
            is PlateForm.Windows -> Charset.forName("GBK")
            else -> Charsets.UTF_8
        }
    }

    suspend fun ping(ip: String): Boolean {
        val pingCommand: Array<String> = when (currentPlateForm) {
            is PlateForm.Windows -> arrayOf("ping", ip, "-n", "1")
            is PlateForm.Linux -> arrayOf("ping", ip, "-c", "1")
            else -> null
        } ?: return false
        val ping = exec(*pingCommand, charset = charset).getOrNull()
        return !(ping == null || !ping.contains("ttl=", true))
    }

}