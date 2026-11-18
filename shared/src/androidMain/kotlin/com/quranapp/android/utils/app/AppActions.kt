/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.app

import android.content.Context
import android.content.ContextWrapper
import com.quranapp.android.backup.AutoBackup
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.scheduling.RecommendedReminderScheduler
import com.quranapp.android.scheduling.VerseOfTheDayScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppActions {
    const val APP_ACTION_KEY = "app.action.key"
    const val APP_ACTION_VICTIM_KEY = "app.action.victim_key"
    const val APP_ACTION_TRANSL_UPDATE = "app.action.translation.update"
    const val APP_ACTION_URLS_UPDATE = "app.action.urls_update"

    private fun updateTransl(ctx: ContextWrapper, slug: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val repo = DatabaseProvider.getTranslationRepository()
            val translationExists = repo.isDownloaded(slug)

            if (translationExists) {
                val bookInfo = repo.getBook(slug)

                // The slug could be empty. Check repo.getBook(slug) for more info.
                if (bookInfo.slug.isNotEmpty()) {
                    PlatformServices.downloads.downloadTranslation(bookInfo)
                }
            }
        }
    }

    @JvmStatic
    fun scheduleActions(ctx: Context) {
        if (VersePreferences.getVOTDReminderEnabled()) {
            VerseOfTheDayScheduler.scheduleDailyNotification(ctx)
            RecommendedReminderScheduler.schedule(ctx)
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (!BackupPreferences.isEnabled()) return@launch

            AutoBackup.ensureAvailable(ctx)
            AutoBackup.schedule(ctx)
        }
    }
}
