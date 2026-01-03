/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.tool.log

import androidx.compose.runtime.snapshots.SnapshotStateList
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import com.virogu.core.Common
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.LoggerFactory

class LogToolImpl : LogTool() {

    private val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    override val logs: SnapshotStateList<ILoggingEvent> = SnapshotStateList()

    override val loggingEventFlow: MutableSharedFlow<ILoggingEvent> = MutableSharedFlow()

    private val logListLevel: Level by lazy {
        if (Common.isDebug) {
            Level.ALL
        } else {
            Level.INFO
        }
    }

    init {
        this.context = rootLogger.loggerContext
    }

    override fun start() {
        super.start()
        rootLogger.addAppender(this)
        println("log started")
    }

    override fun append(eventObject: ILoggingEvent) {
        if (eventObject.level.levelInt < logListLevel.levelInt) {
            return
        }
        if (logs.size >= 2000) {
            runCatching {
                logs.removeRange(1500, logs.size)
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