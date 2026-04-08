package com.quranapp.android.utils.app

import android.content.Context
import androidx.compose.runtime.Composable
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs

enum class ResourceDownloadProxy(val value: String) {
    ALFAAZ_PLUS("alfaazplus"),
    GITHUB("github"),
    JSDELIVR("jsdelivr");

    companion object {
        val DEFAULT = ALFAAZ_PLUS

        fun fromValue(value: String): ResourceDownloadProxy {
            return ResourceDownloadProxy.entries.firstOrNull { it.value == value } ?: ALFAAZ_PLUS
        }
    }
}

object DownloadSourceUtils {
    @Composable
    fun observeCurrentSourceName(): String {
        return getDownloadSourceName(AppPreferences.observeResourceDownloadProxy())
    }

    fun getDownloadSourceName(src: ResourceDownloadProxy): String {
        return when (src) {
            ResourceDownloadProxy.ALFAAZ_PLUS -> "gh-proxy.alfaazplus.com"
            ResourceDownloadProxy.GITHUB -> "raw.githubusercontent.com"
            ResourceDownloadProxy.JSDELIVR -> "cdn.jsdelivr.net"
        }
    }

    fun getDownloadSourceRoot(): String {
        return when (AppPreferences.getResourceDownloadProxy()) {
            ResourceDownloadProxy.ALFAAZ_PLUS -> ApiConfig.GH_PROXY_ROOT
            ResourceDownloadProxy.GITHUB -> ApiConfig.GH_RAW_ROOT
            ResourceDownloadProxy.JSDELIVR -> ApiConfig.JS_DELIVR_ROOT
        }
    }

    fun getDownloadSourceBaseUrl(): String {
        return when (AppPreferences.getResourceDownloadProxy()) {
            ResourceDownloadProxy.ALFAAZ_PLUS -> ApiConfig.GH_PROXY_BASE_URL
            ResourceDownloadProxy.GITHUB -> ApiConfig.GH_RAW_BASE_URL
            ResourceDownloadProxy.JSDELIVR -> ApiConfig.JS_DELIVR_BASE_URL
        }
    }

    suspend fun setDownloadSource(downloadSrc: ResourceDownloadProxy) {
        AppPreferences.setResourceDownloadProxy(downloadSrc)
        resetDownloadSourceBaseUrl()
    }

    @JvmStatic
    fun resetDownloadSourceBaseUrl() {
        RetrofitInstance.githubProxyBaseUrl = getDownloadSourceBaseUrl()
        RetrofitInstance.githubLikeProxyBaseUrl = getDownloadSourceRoot()
        RetrofitInstance.resetGithubApi()
    }
}