package com.alfaazplus.sunnah.ui.utils.shared_preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking


private const val DATASTORE_NAME = "app_preferences"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

data class PrefKey<T>(
    val key: Preferences.Key<T>,
    val default: T
)

class PrefResult(private val map: Map<PrefKey<*>, Any?>) {
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: PrefKey<T>): T {
        return map[key] as T
    }
}

object DataStoreManager {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val dataFlow by lazy {
        appContext.dataStore.data
            .distinctUntilChanged()
            .shareIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    }

    fun <T> read(prefKey: PrefKey<T>): T {
        return read(prefKey.key, prefKey.default)
    }

    fun <T> read(key: Preferences.Key<T>, defaultValue: T): T {
        return runBlocking {
            val preferences = appContext.dataStore.data.first()
            preferences[key] ?: defaultValue
        }
    }

    suspend fun <T> write(prefKey: PrefKey<T>, value: T) {
        write(prefKey.key, value)
    }

    suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        appContext.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }


    suspend fun <T> remove(prefKey: PrefKey<T>) {
        remove(prefKey.key)
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        appContext.dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    suspend fun removeAll(vararg keys: Preferences.Key<*>) {
        appContext.dataStore.edit { preferences ->
            keys.forEach {
                preferences.remove(it)
            }
        }
    }

    suspend fun <T> contains(key: Preferences.Key<T>): Boolean {
        val preferences = appContext.dataStore.data.first()
        return preferences.contains(key)
    }

    fun <T> flow(prefKey: PrefKey<T>): Flow<T> {
        return flow(prefKey.key, prefKey.default)
    }

    fun <T> flow(
        key: Preferences.Key<T>,
        defaultValue: T
    ): Flow<T> {
        return dataFlow
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    fun flowMultiple(vararg keys: PrefKey<*>): Flow<PrefResult> {
        return dataFlow
            .map { preferences ->
                val map = keys.associateWith { prefKey ->
                    preferences[prefKey.key] ?: prefKey.default
                }
                PrefResult(map)
            }
            .distinctUntilChanged()
    }

    @Composable
    fun <T> observe(prefKey: PrefKey<T>): T {
        return observe(prefKey.key, prefKey.default)
    }

    @Composable
    fun <T> observe(
        key: Preferences.Key<T>,
        defaultValue: T
    ): T {
        val flow = remember(key) {
            dataFlow
                .map { it[key] ?: defaultValue }
                .distinctUntilChanged()
        }

        return flow.collectAsStateWithLifecycle(initialValue = defaultValue).value
    }
}
