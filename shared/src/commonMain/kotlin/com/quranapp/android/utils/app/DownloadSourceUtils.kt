package com.quranapp.android.utils.app

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.preferences.AppPreferences


enum class ResourceDownloadProxy(val value: String) {
    ALFAAZ_PLUS("alfaazplus"),
    GITHUB("github"),
    JSDELIVR("jsdelivr"),
    ;

    companion object {
        val DEFAULT = ALFAAZ_PLUS

        fun fromValue(value: String): ResourceDownloadProxy {
            return entries.firstOrNull { it.value == value } ?: ALFAAZ_PLUS
        }
    }
}

object DownloadSourceUtils {
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

    fun resetDownloadSourceBaseUrl() {
        ApiClient.githubProxyBaseUrl = getDownloadSourceBaseUrl()
        ApiClient.githubLikeProxyBaseUrl = getDownloadSourceRoot()
        ApiClient.resetGithubApi()
    }
}
