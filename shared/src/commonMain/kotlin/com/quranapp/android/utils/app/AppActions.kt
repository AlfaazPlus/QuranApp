/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */
package com.quranapp.android.utils.app

import com.quranapp.android.backup.AutoBackup
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.utils.scheduling.RecommendedReminderScheduler
import com.quranapp.android.utils.scheduling.VerseOfTheDayScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppActions {
    fun scheduleActions() {
        if (VersePreferences.getVOTDReminderEnabled()) {
            VerseOfTheDayScheduler.scheduleDailyNotification()
            RecommendedReminderScheduler.schedule()
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (!BackupPreferences.isEnabled()) return@launch

            AutoBackup.ensureAvailable()
            AutoBackup.schedule()
        }
    }
}
