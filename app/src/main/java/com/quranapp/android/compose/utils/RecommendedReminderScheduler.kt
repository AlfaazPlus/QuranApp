package com.quranapp.android.compose.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quranapp.android.utils.workers.RecommendedReminderWorker
import java.util.concurrent.TimeUnit

object RecommendedReminderScheduler {
    private const val ID = "recommended_reminder"

    fun schedule(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<RecommendedReminderWorker>(
            repeatInterval = 1,
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

    fun cancel(context: Context) {
        WorkManager
            .getInstance(context)
            .cancelUniqueWork(ID)
    }
}
