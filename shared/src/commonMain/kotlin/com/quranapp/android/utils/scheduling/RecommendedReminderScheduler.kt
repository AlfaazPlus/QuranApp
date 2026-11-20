package com.quranapp.android.utils.scheduling

import com.quranapp.android.platform.Platform
import com.quranapp.android.task.ExistingPeriodicTaskPolicy
import com.quranapp.android.task.TaskConstraints
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskSchedule
import com.quranapp.android.utils.workers.RecommendedReminderWorker
import kotlin.time.Duration.Companion.hours

object RecommendedReminderScheduler {
    private const val ID = "recommended_reminder"

    fun schedule() {
        Platform.system.taskManager.enqueuePeriodic(
            uniqueName = ID,
            policy = ExistingPeriodicTaskPolicy.KEEP,
            request = TaskRequest(
                workerClass = RecommendedReminderWorker::class,
                constraints = TaskConstraints.NONE,
                schedule = TaskSchedule.Periodic(1.hours),
            ),
        )
    }

    fun cancel() {
        Platform.system.taskManager.cancelUnique(ID)
    }
}
