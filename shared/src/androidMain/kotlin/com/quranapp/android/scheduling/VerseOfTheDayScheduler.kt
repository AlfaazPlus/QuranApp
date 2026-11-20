package com.quranapp.android.scheduling

import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.task.ExistingPeriodicTaskPolicy
import com.quranapp.android.task.TaskConstraints
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskSchedule
import com.quranapp.android.utils.workers.VerseOfTheDayWorker
import kotlin.time.Duration.Companion.hours

object VerseOfTheDayScheduler {
    private const val ID = "votd_reminder"

    fun scheduleDailyNotification() {
        PlatformServices.system.taskManager.enqueuePeriodic(
            uniqueName = ID,
            policy = ExistingPeriodicTaskPolicy.KEEP,
            request = TaskRequest(
                workerClass = VerseOfTheDayWorker::class,
                constraints = TaskConstraints.NONE,
                schedule = TaskSchedule.Periodic(24.hours),
            ),
        )
    }

    fun cancelDailyNotification() {
        PlatformServices.system.taskManager.cancelUnique(ID)
    }
}
