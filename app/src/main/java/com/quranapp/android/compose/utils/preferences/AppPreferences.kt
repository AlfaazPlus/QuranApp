@file:OptIn(ExperimentalCoroutinesApi::class)

package com.quranapp.android.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.utils.app.ResourceDownloadProxy
import kotlinx.coroutines.ExperimentalCoroutinesApi

object AppPreferences {
    private val KEY_DEVELOPER_MODE = PrefKey(booleanPreferencesKey("developer_mode"), false)
    private val KEY_DOWNLOAD_PROXY = PrefKey(
        stringPreferencesKey("resource_download_proxy"),
        ResourceDownloadProxy.DEFAULT.name,
    )

    fun getDeveloperModeEnabled(): Boolean {
        return DataStoreManager.read(KEY_DEVELOPER_MODE)
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_DEVELOPER_MODE, enabled)
    }

    @Composable
    fun observeDeveloperModeEnabled(): Boolean {
        return DataStoreManager.observe(KEY_DEVELOPER_MODE)
    }

    fun getResourceDownloadProxy(): ResourceDownloadProxy {
        return DataStoreManager.read(KEY_DOWNLOAD_PROXY).let { ResourceDownloadProxy.fromValue(it) }
    }

    suspend fun setResourceDownloadProxy(src: ResourceDownloadProxy) {
        DataStoreManager.write(KEY_DOWNLOAD_PROXY, src.value)
    }

    @Composable
    fun observeResourceDownloadProxy(): ResourceDownloadProxy {
        return DataStoreManager.observe(KEY_DOWNLOAD_PROXY)
            .let { ResourceDownloadProxy.fromValue(it) }
    }
}
