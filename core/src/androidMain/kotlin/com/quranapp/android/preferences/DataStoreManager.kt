package com.quranapp.android.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath

private const val DATASTORE_NAME = "app_preferences"

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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cachedPreferences = MutableStateFlow<Preferences?>(null)
    private lateinit var preferences: DataStore<Preferences>

    fun init(context: Context) {
        appContext = context.applicationContext

        if (!::preferences.isInitialized) {
            val path =
                appContext.filesDir.toOkioPath() / "datastore" / "$DATASTORE_NAME.preferences_pb"

            preferences = PreferenceDataStoreFactory.createWithPath(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { path },
            )
        }

        if (cachedPreferences.value == null) {
            scope.launch {
                preferences.data.collect { cachedPreferences.value = it }
            }
        }
    }

    private val dataFlow by lazy {
        cachedPreferences
            .filterNotNull()
            .distinctUntilChanged()
            .shareIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    }

    /**
     * Returns the most recently loaded value without blocking the caller. Until DataStore has
     * completed its first read, the supplied default is returned. Use [readFirst] when a
     * background operation must wait for the persisted value.
     */
    fun <T> read(prefKey: PrefKey<T>): T {
        return read(prefKey.key, prefKey.default)
    }

    /**
     * See [read]. This deliberately never performs disk I/O on the calling thread.
     */
    fun <T> read(key: Preferences.Key<T>, defaultValue: T): T {
        return cachedPreferences.value?.get(key) ?: defaultValue
    }

    suspend fun <T> readFirst(prefKey: PrefKey<T>): T {
        return readFirst(prefKey.key, prefKey.default)
    }

    suspend fun <T> readFirst(key: Preferences.Key<T>, defaultValue: T): T {
        val preferences = preferences.data.first()
        return preferences[key] ?: defaultValue
    }

    suspend fun <T> write(prefKey: PrefKey<T>, value: T) {
        write(prefKey.key, value)
    }

    suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        preferences.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Single DataStore transaction — observers (e.g. [flowMultiple]) emit once with all keys updated.
     */
    suspend fun edit(transform: suspend MutablePreferences.() -> Unit) {
        preferences.edit { it.transform() }
    }

    suspend fun <T> remove(prefKey: PrefKey<T>) {
        remove(prefKey.key)
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        preferences.edit { preferences ->
            preferences.remove(key)
        }
    }

    suspend fun removeAll(vararg keys: Preferences.Key<*>) {
        preferences.edit { preferences ->
            keys.forEach {
                preferences.remove(it)
            }
        }
    }

    suspend fun <T> contains(key: Preferences.Key<T>): Boolean {
        val preferences = preferences.data.first()
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
}
