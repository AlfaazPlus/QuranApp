/*
 * (c) Faisal Khan. Created on 21/11/2021.
 */
package com.quranapp.android.utils.app

import android.content.Context
import android.util.TypedValue
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.quranapp.android.api.models.AppUrls
import kotlinx.coroutines.CancellationException

object InfoUtils {
    suspend fun open(context: Context, urlKey: String) {
        try {
            val url = UrlsManager(context).load().url(urlKey)
            if (url.isNullOrEmpty()) return
            prepareCustomTab(context).launchUrl(context, url.toUri())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.saveError(e, "InfoUtils.open")
            throw e
        }
    }

    private fun AppUrls.url(key: String): String? = when (key) {
        UrlsManager.URL_KEY_FEEDBACK -> feedback
        UrlsManager.URL_KEY_PRIVACY_POLICY -> privacyPolicy
        UrlsManager.URL_KEY_ABOUT -> about
        UrlsManager.URL_KEY_HELP -> help
        UrlsManager.URL_KEY_DISCORD -> discord
        UrlsManager.URL_KEY_DONATION -> donation
        else -> null
    }

    private fun prepareCustomTab(context: Context): CustomTabsIntent {
        val pageColor = TypedValue().let { value ->
            context.theme.resolveAttribute(android.R.attr.statusBarColor, value, true)
            value.data
        }
        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(pageColor)
            .setNavigationBarColor(pageColor)
            .build()

        return CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorSchemeParams)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
    }
}
