package com.virogu.core.device.ability.adb

import com.virogu.core.PlateForm
import com.virogu.core.bean.ScrcpyConfig
import com.virogu.core.command.BaseCommand
import com.virogu.core.commonWorkDir
import com.virogu.core.currentPlateForm
import com.virogu.core.device.Device
import com.virogu.core.device.ability.DeviceAbilityScrcpy
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val workDir: File by lazy {
        commonWorkDir.resolve("app")
    }

    private val executable by lazy {
        when (currentPlateForm) {
            is PlateForm.Linux -> arrayOf("./scrcpy")
            else -> arrayOf("cmd.exe", "/c", "scrcpy")
        }
    }

    private val environment: Map<String, String> by lazy {
        when (currentPlateForm) {
            is PlateForm.Linux -> mapOf(
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
        val serial = device.serial
        val title = device.showName
        val command = arrayOf(
            *executable, "-s", serial, "--window-title=$title",
            *commonConfig.scrcpyArgs().toTypedArray(),
            *config.scrcpyArgs().toTypedArray(),
        )
        return cmd.execAsync(
            scope,
            *command,
            workDir = workDir,
            env = environment
        ) {
            logger.info(it)
        }
    }
}