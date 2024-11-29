package com.virogu.core.bean

import kotlinx.serialization.Serializable
import java.io.File

/**
 * [ConfigDoc](https://github.com/Genymobile/scrcpy/blob/master/doc/video.md)
 */
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
        fun scrcpyArgs() = listOfNotNull(
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

    @Serializable
    data class Config(
        //--max-size=1024  -m 1024
        val maxSize: MaxSize = MaxSize.Default,
        //--video-bit-rate=2M -b 2M
        val bitRate: VideoBiteRate = VideoBiteRate.M8,
        //--video-codec=h264
        val videoCodec: VideoCodec = VideoCodec.H264,
        //--capture-orientation
        val videoRotation: VideoRotation = VideoRotation.Default,
        //--orientation=0
        val windowRotation: WindowRotation = WindowRotation.R0
    ) {
        fun scrcpyArgs() = listOfNotNull(
            "--max-size=${maxSize.value}".takeIf { maxSize != MaxSize.Default },
            "--video-bit-rate=${bitRate.value}",
            "--video-codec=${videoCodec.value}",
            "--capture-orientation=${videoRotation.value}".takeIf { videoRotation != VideoRotation.Default },
            "--orientation=${windowRotation.value}"
        )
    }

    enum class MaxSize(val value: String) {
        Default("原始"),
        W640("640"),
        W720("720"),
        W1080("1080"),
        W1280("1280"),
        W1920("1920"),
    }

    enum class VideoBiteRate(val value: String) {
        M4("4M"),
        M8("8M"),
        M20("20M"),
        M50("50M"),
        M100("100M")
    }

    enum class VideoCodec(val value: String) {
        H264("h264"),
        H265("h265"),
        AV1("av1"),
    }

    enum class RecordFormat(val value: String) {
        MP4("mp4"),
        MKV("mkv"),
    }

    enum class VideoRotation(val value: String, val desc: String) {
        Default("0", "默认"),
        R0("0", "0°"),
        R1("90", "90°"),
        R2("180", "180°"),
        R3("270", "270°"),
    }

    enum class WindowRotation(val value: String, val desc: String) {
        R0("0", "默认"),
        R1("90", "90°"),
        R2("180", "180°"),
        R3("270", "270°"),
    }

}