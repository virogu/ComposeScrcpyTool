package com.virogu.bean

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ScrcpyConfig(
    val commonConfig: CommonConfig = CommonConfig(),
    val configs: Map<String, Config> = emptyMap()
) {

    @Serializable
    data class CommonConfig(
        //--record=file.mp4
        //--record=file --record-format=mp4
        val recordEnable: Boolean = false,
        val recordPath: String = "",
        //--record-format=mp4
        val recordFormat: RecordFormat = RecordFormat.MP4,
        //--always-on-top
        val alwaysOnTop: Boolean = false,
        //--no-audio
        val enableAudio: Boolean = false,
        //--stay-awake -w
        val stayAwake: Boolean = false,
        //--turn-screen-off -S
        val turnScreenOff: Boolean = false,
        //--show-touches
        val showTouches: Boolean = false,
        //--window-borderless
        val noWindowBorder: Boolean = false
    ) {
        fun args() = listOfNotNull(
            "--no-audio".takeIf { !enableAudio },
            if (recordEnable && File(recordPath).let {
                    it.exists() && it.isDirectory
                }
            ) {
                "--record=${recordPath}/scrcpy_record_${System.currentTimeMillis()}.${recordFormat.value}"
            } else {
                null
            },
            "--stay-awake".takeIf { stayAwake },
            "--turn-screen-off".takeIf { turnScreenOff },
            "--show-touches".takeIf { showTouches },
            "--window-borderless".takeIf { noWindowBorder },
            "--always-on-top".takeIf { alwaysOnTop }
        )
    }

    enum class RecordFormat(val value: String) {
        MP4("mp4"),
        MKV("mkv"),
    }

    @Serializable
    class Config(

    ) {
        fun args() = listOfNotNull<String>(

        )
    }

}