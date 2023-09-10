/*
 * (c) Faisal Khan. Created on 21/11/2021.
 */
package com.quranapp.android.utils.app

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.widgets.dialog.loader.PeaceProgressDialog
import java.util.concurrent.CancellationException

object InfoUtils {
    @JvmStatic
    fun openFeedbackPage(context: Context) {
        openTab(context, UrlsManager.URL_KEY_FEEDBACK)
    }

    @JvmStatic
    fun openPrivacyPolicy(context: Context) {
        openTab(context, UrlsManager.URL_KEY_PRIVACY_POLICY)
    }

    @JvmStatic
    fun openAbout(context: Context) {
        openTab(context, UrlsManager.URL_KEY_ABOUT)
    }

    @JvmStatic
    fun openHelp(context: Context) {
        openTab(context, UrlsManager.URL_KEY_HELP)
    }

    @JvmStatic
    fun openDiscord(context: Context) {
        openTab(context, UrlsManager.URL_KEY_DISCORD)
    }

    private fun openTab(context: Context, urlKey: String) {
        val urlsManager = UrlsManager(context)
        val dialog = PeaceProgressDialog(context).apply {
            setMessage(R.string.strTextPleaseWait)
            setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.strLabelCancel)) { _, _ ->
                urlsManager.cancel()
                dismiss()
            }
            show()
        }

        val failedCallback = { e: Exception ->
            e.printStackTrace()
            dialog.dismiss()
            if (e !is CancellationException) {
                Logger.reportError(e)
                MessageUtils.popMessage(
                        context,
                        context.getString(R.string.strMsgSomethingWrong),
                        "${context.getString(R.string.strMsgCouldNotOpenPage)} ${
                            context.getString(
                                    R.string.strMsgTryLater
                            )
                        }",
                        context.getString(R.string.strLabelClose),
                        null
                )
            }
        }

        urlsManager.getUrlsJson({ (privacyPolicy, about, help, feedback, discord): AppUrls ->
            val url: String? = when (urlKey) {
                UrlsManager.URL_KEY_FEEDBACK -> feedback
                UrlsManager.URL_KEY_PRIVACY_POLICY -> privacyPolicy
                UrlsManager.URL_KEY_ABOUT -> about
                UrlsManager.URL_KEY_HELP -> help
                UrlsManager.URL_KEY_DISCORD -> discord
                else -> null
            }

            try {
                prepareCustomTab(context).launchUrl(context, Uri.parse(url))
                dialog.dismiss()
            } catch (e: Exception) {
                failedCallback(e)
            }
        }, failedCallback)
    }

    private fun prepareCustomTab(context: Context): CustomTabsIntent {
        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.colorBGPage))
                .setNavigationBarColor(ContextCompat.getColor(context, R.color.colorBGPage))
                .build()

        return CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorSchemeParams)
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
    }
}
