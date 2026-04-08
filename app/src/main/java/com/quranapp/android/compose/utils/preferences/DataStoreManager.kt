package com.alfaazplus.sunnah.ui.utils.shared_preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    fun <T> flow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        val dValue = read(key, defaultValue)

        return appContext.dataStore.data.map { preferences ->
            preferences[key] ?: dValue
        }
    }

    fun flowMultiple(vararg keys: PrefKey<*>): Flow<PrefResult> {
        return appContext.dataStore.data.map { preferences ->
            val map = keys.associateWith { prefKey ->
                preferences[prefKey.key] ?: prefKey.default
            }

            PrefResult(map)
        }
    }

    @Composable
    fun <T> observe(prefKey: PrefKey<T>): T {
        return observe(prefKey.key, prefKey.default)
    }

    @Composable
    fun <T> observe(key: Preferences.Key<T>, defaultValue: T): T {
        return flow(key, defaultValue)
            .collectAsStateWithLifecycle(defaultValue)
            .value
    }

    suspend fun <T> observeWithCallback(key: Preferences.Key<T>, onChange: (T) -> Unit) {
        appContext.dataStore.data.collect { preferences ->
            val value = preferences[key]
            value?.let { onChange(it) }
        }
    }
}
