package com.quranapp.android.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.utils.app.ResourceDownloadProxy

object AppPreferences {
    val KEY_LAST_CRASH_LOG_FILENAME = PrefKey(stringPreferencesKey("last_crash_log_filename"), "")
    val KEY_DEVELOPER_MODE = PrefKey(booleanPreferencesKey("developer_mode"), false)
    val KEY_DOWNLOAD_PROXY = PrefKey(
        stringPreferencesKey("resource_download_proxy"),
        ResourceDownloadProxy.DEFAULT.name,
    )

    fun getLastCrashLogFilename(): String? {
        return DataStoreManager.read(KEY_LAST_CRASH_LOG_FILENAME).takeIf { it.isNotEmpty() }
    }

    suspend fun setLastCrashLogFileName(filename: String) {
        DataStoreManager.write(KEY_LAST_CRASH_LOG_FILENAME, filename)
    }

    suspend fun removeLastCrashLogFilename() {
        DataStoreManager.remove(KEY_LAST_CRASH_LOG_FILENAME)
    }

    fun getDeveloperModeEnabled(): Boolean {
        return DataStoreManager.read(KEY_DEVELOPER_MODE)
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_DEVELOPER_MODE, enabled)
    }

    fun getResourceDownloadProxy(): ResourceDownloadProxy {
        return DataStoreManager.read(KEY_DOWNLOAD_PROXY).let { ResourceDownloadProxy.fromValue(it) }
    }

    suspend fun setResourceDownloadProxy(src: ResourceDownloadProxy) {
        DataStoreManager.write(KEY_DOWNLOAD_PROXY, src.value)
    }

    @Composable
    fun observeDeveloperModeEnabled(): Boolean {
        return DataStoreManager.observe(KEY_DEVELOPER_MODE)
    }

    @Composable
    fun observeResourceDownloadProxy(): ResourceDownloadProxy {
        return DataStoreManager.observe(KEY_DOWNLOAD_PROXY)
            .let { ResourceDownloadProxy.fromValue(it) }
    }
}
