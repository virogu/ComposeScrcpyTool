package com.virogu.tools.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.virogu.tools.config.datastore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

abstract class BaseConfigStore(protected val dataStore: DataStore<Preferences>) {

    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    protected fun <T> getConfig(
        key: Preferences.Key<T>, defaultValue: T
    ): Flow<T> = dataStore.getConfig(key, defaultValue)

    protected fun <T> updateConfig(key: Preferences.Key<T>, value: T) = dataStore.updateConfig(key, value)

    protected inline fun <reified T> getSerializableConfig(
        key: Preferences.Key<String>, defaultValue: T
    ): Flow<T> = dataStore.getSerializableConfig(key, defaultValue)

    protected inline fun <reified T> updateSerializableConfig(
        key: Preferences.Key<String>, value: T
    ) = dataStore.updateSerializableConfig(key, value)

    protected fun <T> clearConfig(key: Preferences.Key<T>) = dataStore.clearConfig(key)

}