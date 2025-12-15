package com.alfaazplus.sunnah.ui.utils.shared_preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking


private const val DATASTORE_NAME = "app_preferences"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object DataStoreManager {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        appContext.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun <T> read(key: Preferences.Key<T>, defaultValue: T): T {
        return runBlocking {
            val preferences = appContext.dataStore.data.first()
            preferences[key] ?: defaultValue
        }
    }

    suspend fun <T> contains(key: Preferences.Key<T>): Boolean {
        val preferences = appContext.dataStore.data.first()
        return preferences.contains(key)
    }

    @Composable
    fun <T> observe(key: Preferences.Key<T>, defaultValue: T): T {
        val dValue = read(key, defaultValue)

        return appContext.dataStore.data.map { preferences ->
            preferences[key] ?: dValue
        }.collectAsStateWithLifecycle(dValue).value
    }

    suspend fun <T> observeWithCallback(key: Preferences.Key<T>, onChange: (T) -> Unit) {
        appContext.dataStore.data.collect { preferences ->
            val value = preferences[key]
            value?.let { onChange(it) }
        }
    }
}
