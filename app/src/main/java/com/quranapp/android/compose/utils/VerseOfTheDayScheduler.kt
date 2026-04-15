package com.quranapp.android.compose.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quranapp.android.utils.workers.VerseOfTheDayWorker
import java.util.concurrent.TimeUnit

object VerseOfTheDayScheduler {
    private const val ID = "votd_reminder"

    fun scheduleDailyNotification(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<VerseOfTheDayWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                ID,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest,
            )
    }

    fun cancelDailyNotification(context: Context) {
        WorkManager
            .getInstance(context)
            .cancelUniqueWork(ID)
    }
}