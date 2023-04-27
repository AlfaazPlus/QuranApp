/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.app

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import com.peacedesign.android.utils.AppBridge
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.services.TranslationDownloadService
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import com.quranapp.android.utils.sharedPrefs.SPLog
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
        val factory = QuranTranslationFactory(ctx)
        val translationExists = factory.isTranslationDownloaded(slug)
        if (translationExists) {
            val bookInfo = factory.getTranslationBookInfo(slug)

            // The slug could be empty. Check factory.getTranslationBookInfo(slug) for more info.
            if (bookInfo.slug.isNotEmpty()) {
                TranslationDownloadService.startDownloadService(ctx, bookInfo)
            }
        }
        factory.close()
    }

    @JvmStatic
    fun scheduleActions(ctx: Context) {
        if (VOTDUtils.isVOTDTrulyEnabled(ctx)) {
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
                val (urlsVersion, translationsVersion, recitationsVersion, recitationsTranslationVersion, tafsirsVersion)
                        = RetrofitInstance.github.getResourcesVersions()

                val localUrlsVersion = SPAppConfigs.getUrlsVersion(ctx)
                val localTranslationsVersion = SPAppConfigs.getTranslationsVersion(ctx)
                val localRecitationsVersion = SPAppConfigs.getRecitationsVersion(ctx)
                val localRecitationTranslationsVersion = SPAppConfigs.getRecitationTranslationsVersion(ctx)
                val localTafsirsVersion = SPAppConfigs.getTafsirsVersion(ctx)

                if (urlsVersion > localUrlsVersion) {
                    SPAppActions.setFetchUrlsForce(ctx, true)
                    SPAppConfigs.setUrlsVersion(ctx, urlsVersion)
                }

                if (translationsVersion > localTranslationsVersion) {
                    SPAppActions.setFetchTranslationsForce(ctx, true)
                    SPAppConfigs.setTranslationsVersion(ctx, translationsVersion)
                }

                if (recitationsVersion > localRecitationsVersion) {
                    SPAppActions.setFetchRecitationsForce(ctx, true)
                    SPAppConfigs.setRecitationsVersion(ctx, recitationsVersion)
                }

                if (recitationsTranslationVersion > localRecitationTranslationsVersion) {
                    SPAppActions.setFetchRecitationTranslationsForce(ctx, true)
                    SPAppConfigs.setRecitationTranslationsVersion(ctx, recitationsVersion)
                }

                if (tafsirsVersion > localTafsirsVersion) {
                    SPAppActions.setFetchTafsirsForce(ctx, true)
                    SPAppConfigs.setTafsirsVersion(ctx, tafsirsVersion)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun checkForCrashLogs(ctx: Context) {
        var lastCrashLog = Log.getLastCrashLog(ctx)

        if (lastCrashLog.isNullOrEmpty()) return

        val length = lastCrashLog.length
        if (length > 300) {
            lastCrashLog = lastCrashLog.substring(0, 300) + "... ${length - 300} more chars"
        }

        PeaceDialog.newBuilder(ctx)
            .setTitle(R.string.lastCrashLog)
            .setMessage(lastCrashLog)
            .setNeutralButton(R.string.strLabelCopy) { _, _ ->
                ctx.copyToClipboard(lastCrashLog)
                Toast.makeText(ctx, R.string.copiedToClipboard, Toast.LENGTH_LONG).show()
                SPLog.removeLastCrashLogFilename(ctx)
            }
            .setPositiveButton(R.string.createIssue) { _, _ ->
                ctx.copyToClipboard(lastCrashLog)
                SPLog.removeLastCrashLogFilename(ctx)
                Toast.makeText(ctx, R.string.pasteCrashLogGithubIssue, Toast.LENGTH_LONG).show()
                AppBridge.newOpener(ctx).browseLink(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL)
            }
            .show()
    }
}
