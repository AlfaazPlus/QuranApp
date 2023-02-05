/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.app

import android.content.Context
import android.content.ContextWrapper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.reader.factory.QuranTranslFactory
import com.quranapp.android.utils.services.TranslDownloadService
import com.quranapp.android.utils.sp.SPAppActions
import com.quranapp.android.utils.sp.SPAppConfigs
import com.quranapp.android.utils.sp.SPVerses
import com.quranapp.android.utils.votd.VOTDUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppActions {
    const val APP_ACTION_KEY = "app.action.key"
    const val APP_ACTION_VICTIM_KEY = "app.action.victim_key"
    const val APP_ACTION_TRANSL_UPDATE = "app.action.translation.update"
    const val APP_ACTION_URLS_UPDATE = "app.action.urls_update"

    private fun updateTransl(ctx: ContextWrapper, slug: String) {
        val factory = QuranTranslFactory(ctx)
        val translationExists = factory.isTranslationDownloaded(slug)
        if (translationExists) {
            val bookInfo = factory.getTranslationBookInfo(slug)

            // The slug could be empty. Check factory.getTranslationBookInfo(slug) for more info.
            if (bookInfo.slug.isNotEmpty()) {
                TranslDownloadService.startDownloadService(ctx, bookInfo)
            }
        }
        factory.close()
    }

    @JvmStatic
    fun scheduleActions(ctx: Context) {
        if (SPVerses.getVOTDReminderEnabled(ctx)) {
            VOTDUtils.enableVOTDReminder(ctx)
        }
    }

    /**
     * Checks if there has been changes in the app resources on the server.
     * If there has been changes, then the upcoming versions from the remote config will be greater than that of locally saved.
     * */
    @JvmStatic
    fun checkForResourcesVersions(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (urlsVersion, translationsVersion, recitationsVersion) = RetrofitInstance.github.getResourcesVersions()

                val localUrlsVersion = SPAppConfigs.getUrlsVersion(ctx)
                val localTranslationsVersion = SPAppConfigs.getTranslationsVersion(ctx)
                val localRecitationsVersion = SPAppConfigs.getRecitationsVersion(ctx)

                Logger.print("RESOURCE VERSIONS: URLs: local: $localUrlsVersion, server: $urlsVersion")
                if (urlsVersion > localUrlsVersion) {
                    SPAppActions.setFetchUrlsForce(ctx, true)
                    SPAppConfigs.setUrlsVersion(ctx, urlsVersion)
                    Logger.print("Updated URLs version from $localUrlsVersion to $urlsVersion")
                }

                Logger.print("RESOURCE VERSIONS: TRANSLATIONS: local: $localTranslationsVersion, server: $translationsVersion")
                if (translationsVersion > localTranslationsVersion) {
                    SPAppActions.setFetchTranslationsForce(ctx, true)
                    SPAppConfigs.setTranslationsVersion(ctx, translationsVersion)
                    Logger.print("Updated translations version from $localTranslationsVersion to $translationsVersion")
                }

                Logger.print("RESOURCE VERSIONS: RECITATIONS: local: $localRecitationsVersion, server: $recitationsVersion")
                if (recitationsVersion > localRecitationsVersion) {
                    SPAppActions.setFetchRecitationsForce(ctx, true)
                    SPAppConfigs.setRecitationsVersion(ctx, recitationsVersion)
                    Logger.print("Updated recitations version from $localRecitationsVersion to $recitationsVersion")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}