package com.virogu.tools.log

import androidx.compose.runtime.snapshots.SnapshotStateList
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.LoggerFactory

class LogToolImpl : LogTool() {

    private val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    override val logs: SnapshotStateList<ILoggingEvent> = SnapshotStateList()

    override val loggingEventFlow: MutableSharedFlow<ILoggingEvent> = MutableSharedFlow()

    init {
        this.context = rootLogger.loggerContext
    }

    override fun start() {
        super.start()
        rootLogger.addAppender(this)
        println("log started")
    }

    override fun append(eventObject: ILoggingEvent) {
        if (eventObject.level.levelInt < Level.INFO_INT) {
            return
        }
        if (logs.size >= 2000) {
            runCatching {
                logs.removeRange(1800, logs.size)
            }
        }
        logs.add(eventObject)
    }

    override fun stop() {
        rootLogger.detachAppender(this)
        super.stop()
        println("log stop")
    }

}