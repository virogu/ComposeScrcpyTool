package com.virogu.core.tool

import androidx.compose.runtime.snapshots.SnapshotStateList
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import kotlinx.coroutines.flow.SharedFlow

abstract class LogTool : AppenderBase<ILoggingEvent>() {

    abstract val logs: SnapshotStateList<ILoggingEvent>

    abstract val loggingEventFlow: SharedFlow<ILoggingEvent>

}