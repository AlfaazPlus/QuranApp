package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs

object DownloadSourceUtils {
    const val DOWNLOAD_SRC_ALFAAZ_PLUS = "alfaazplus"
    const val DOWNLOAD_SRC_GITHUB = "github"
    const val DOWNLOAD_SRC_JSDELIVR = "jsdelivr"
    const val DOWNLOAD_SRC_DEFAULT = DOWNLOAD_SRC_ALFAAZ_PLUS

    @JvmStatic
    fun getCurrentSourceName(context: Context): String {
        return when (SPAppConfigs.getResourceDownloadSrc(context)) {
            DOWNLOAD_SRC_ALFAAZ_PLUS -> "gh-proxy.alfaazplus.com"
            DOWNLOAD_SRC_GITHUB -> "raw.githubusercontent.com"
            DOWNLOAD_SRC_JSDELIVR -> "cdn.jsdelivr.net"
            else -> ""
        }
    }

    @JvmStatic
    fun getDownloadSourceBaseUrl(context: Context): String {
        return when (SPAppConfigs.getResourceDownloadSrc(context)) {
            DOWNLOAD_SRC_ALFAAZ_PLUS -> ApiConfig.GH_PROXY_ROOT_URL
            DOWNLOAD_SRC_GITHUB -> ApiConfig.GITHUB_ROOT_URL
            DOWNLOAD_SRC_JSDELIVR -> ApiConfig.JS_DELIVR_ROOT_URL
            else -> ApiConfig.GH_PROXY_ROOT_URL
        }
    }

    @JvmStatic
    fun getDownloadSourceId(context: Context): Int {
        return when (SPAppConfigs.getResourceDownloadSrc(context)) {
            DOWNLOAD_SRC_ALFAAZ_PLUS -> R.id.srcAlfaazPlus
            DOWNLOAD_SRC_GITHUB -> R.id.srcGithub
            DOWNLOAD_SRC_JSDELIVR -> R.id.srcJsDelivr
            else -> R.id.srcAlfaazPlus
        }
    }

    @JvmStatic
    fun resetDownloadSourceBaseUrl(ctx: Context) {
        val downloadSrcUrl = getDownloadSourceBaseUrl(ctx)

        if (RetrofitInstance.githubResDownloadUrl != downloadSrcUrl) {
            RetrofitInstance.githubResDownloadUrl = downloadSrcUrl
            RetrofitInstance.resetGithubApi()
        }
    }
}