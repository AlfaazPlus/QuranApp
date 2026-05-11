package com.quranapp.android.compose.utils.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.compose.utils.preferences.DataStoreManager.flowMultiple
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

    fun toKey(): String {
        return map.values.joinToString("|") { it?.toString() ?: "" }
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

    /**
     * Blocking read — uses [runBlocking]. Do not call from the main thread; it can cause ANRs.
     * Prefer [readFirst] from a coroutine instead.
     */
    fun <T> read(prefKey: PrefKey<T>): T {
        return read(prefKey.key, prefKey.default)
    }

    /**
     * Blocking read — uses [runBlocking]. Do not call from the main thread; it can cause ANRs.
     * Prefer [readFirst] from a coroutine instead.
     */
    fun <T> read(key: Preferences.Key<T>, defaultValue: T): T {
        return runBlocking {
            readFirst(key, defaultValue)
        }
    }

    suspend fun <T> readFirst(prefKey: PrefKey<T>): T {
        return readFirst(prefKey.key, prefKey.default)
    }

    suspend fun <T> readFirst(key: Preferences.Key<T>, defaultValue: T): T {
        val preferences = appContext.dataStore.data.first()
        return preferences[key] ?: defaultValue
    }

    suspend fun <T> write(prefKey: PrefKey<T>, value: T) {
        write(prefKey.key, value)
    }

    suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        appContext.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Single DataStore transaction — observers (e.g. [flowMultiple]) emit once with all keys updated.
     */
    suspend fun edit(transform: suspend MutablePreferences.() -> Unit) {
        appContext.dataStore.edit { it.transform() }
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
