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
import com.quranapp.android.compose.utils.RecommendedReminderScheduler
import com.quranapp.android.compose.utils.VerseOfTheDayScheduler
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.managers.TranslationDownloadManager
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.sharedPrefs.SPLog

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
                TranslationDownloadManager.startDownload(ctx, bookInfo)
            }
        }
        factory.close()
    }

    @JvmStatic
    fun scheduleActions(ctx: Context) {
        if (VersePreferences.getVOTDReminderEnabled()) {
            VerseOfTheDayScheduler.scheduleDailyNotification(ctx)
            RecommendedReminderScheduler.schedule(ctx)
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
