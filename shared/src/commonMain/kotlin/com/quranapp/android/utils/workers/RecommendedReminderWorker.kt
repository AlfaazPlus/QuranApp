package com.quranapp.android.utils.workers

import com.quranapp.android.models.ReferenceVerseModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.labelRecommended
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.platform.notification.AppNotification
import com.quranapp.android.platform.notification.NotificationCategory
import com.quranapp.android.platform.notification.NotificationChannels
import com.quranapp.android.platform.notification.NotificationIntent
import com.quranapp.android.platform.notification.NotificationPriority
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.recommended.Recommendation
import com.quranapp.android.utils.reader.recommended.RecommendationRef
import com.quranapp.android.utils.reader.recommended.Recommended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class RecommendedReminderWorker() : TaskWorker {
    companion object {
        private const val NOTIFICATION_ID = 1015
    }

    override suspend fun doWork(context: TaskContext): TaskResult = withContext(Dispatchers.IO) {
        if (!VersePreferences.getVOTDReminderEnabled()) {
            return@withContext TaskResult.Success()
        }

        val recs = Recommended.getRecommendations()
        if (recs.isEmpty()) {
            return@withContext TaskResult.Success()
        }

        val first = recs.first()

        val epochDay = first.notificationDedupeEpochDay
        val sig = dedupeSignature(first)

        if (epochDay == VersePreferences.getRecommendedNotifDedupeEpochDay() &&
            sig == VersePreferences.getRecommendedNotifDedupeSignature()
        ) {
            return@withContext TaskResult.Success()
        }

        val route = routeForRecommendation(first)
            ?: return@withContext TaskResult.Success()

        val body = first.description.trim().ifBlank {
            getString(Res.string.labelRecommended)
        }

        val notification = AppNotification(
            id = NOTIFICATION_ID,
            channelId = NotificationChannels.CHANNEL_ID_RECOMMENDED_REMINDER,
            title = first.title,
            content = body,
            bigText = body,
            category = NotificationCategory.RECOMMENDATION,
            priority = NotificationPriority.HIGH,
            autoCancel = true,
            contentIntent = NotificationIntent.Route(route),
        )

        Platform.system.notificationManager.notify(NOTIFICATION_ID, notification)

        VersePreferences.setRecommendedNotifDedupeState(epochDay, sig)

        TaskResult.Success()
    }

    private fun dedupeSignature(recommendation: Recommendation): String {
        val ref = when (val r = recommendation.reference) {
            is RecommendationRef.Chapter -> "c:${r.number}"
            is RecommendationRef.Verses -> "v:${r.spec}"
        }

        return "${recommendation.title}\u0000$ref"
    }

    private fun routeForRecommendation(
        recommendation: Recommendation,
    ): AppRoute? {
        return when (val ref = recommendation.reference) {
            is RecommendationRef.Chapter -> {
                AppRoute.Reader(
                    ReaderLaunchParams(
                        data = ReaderIntentData.FullChapter(ref.number),
                    )
                )
            }

            is RecommendationRef.Verses -> {
                val ranges = ref.spec.split(',')
                val chapters = mutableSetOf<Int>()
                val verseSpecs = mutableSetOf<String>()

                for (rangeSpec in ranges) {
                    val trimmed = rangeSpec.trim()
                    val chapterNo =
                        trimmed.split(':').firstOrNull()?.toIntOrNull() ?: return null
                    chapters.add(chapterNo)
                    verseSpecs.add(trimmed)
                }

                AppRoute.Screen(
                    screen = AppRoutes.REFERENCE,
                    extras = ReferenceVerseModel(
                        title = recommendation.title,
                        desc = recommendation.description.takeIf { it.isNotBlank() },
                        chapters = chapters,
                        verses = verseSpecs,
                    ).toExtras(),
                )
            }
        }
    }
}
