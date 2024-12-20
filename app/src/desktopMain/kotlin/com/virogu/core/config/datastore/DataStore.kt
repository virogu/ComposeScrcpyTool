/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.virogu.core.config.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.virogu.core.json
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

private val logger = KotlinLogging.logger("DataStore")

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T?
): Flow<T?> = this.data.map {
    it.get(key = key) ?: defaultValue
}.distinctUntilChanged()

fun <T> DataStore<Preferences>.getNotNull(
    key: Preferences.Key<T>,
    defaultValue: T
): Flow<T> = this.data.map {
    it.get(key = key) ?: defaultValue
}.distinctUntilChanged()

fun <T> DataStore<Preferences>.getConfig(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
    return getNotNull(key, defaultValue)
}

fun <T> DataStore<Preferences>.updateConfig(key: Preferences.Key<T>, value: T) {
    runBlocking {
        runCatching {
            edit {
                it[key] = value
            }
        }.onFailure {
            logger.warn { "save config fail. $it" }
        }
    }
}

inline fun <reified T> DataStore<Preferences>.getSerializableConfig(
    key: Preferences.Key<String>, defaultValue: T
): Flow<T> = getConfig(key, "{}").map {
    it.decode(defaultValue)
}.distinctUntilChanged()

inline fun <reified T> DataStore<Preferences>.updateSerializableConfig(key: Preferences.Key<String>, value: T) {
    updateConfig(key, value.encode())
}

fun <T> DataStore<Preferences>.clearConfig(key: Preferences.Key<T>) {
    runBlocking {
        runCatching {
            edit {
                it.remove(key)
            }
        }.onFailure {
            logger.warn { "save config fail. $it" }
        }
    }
}

inline fun <reified T> String.decode(default: T): T {
    return try {
        json.decodeFromString(this)
    } catch (e: Throwable) {
        println("decode fail: $e")
        default
    }
}

inline fun <reified T> T.encode(): String {
    return try {
        json.encodeToString(this)
    } catch (e: Throwable) {
        println("encode fail: $e")
        "{}"
    }
}