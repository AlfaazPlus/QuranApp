package com.quranapp.android.utils.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.recommended.Recommendation
import com.quranapp.android.utils.recommended.RecommendationRef
import com.quranapp.android.utils.recommended.Recommended
import com.quranapp.android.utils.univ.Codes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendedReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!VersePreferences.getVOTDReminderEnabled()) {
            return@withContext Result.success()
        }

        val recs = Recommended.getRecommendations(applicationContext)
        if (recs.isEmpty()) {
            return@withContext Result.success()
        }

        val first = recs.first()

        val epochDay = first.notificationDedupeEpochDay
        val sig = dedupeSignature(first)

        if (epochDay == VersePreferences.getRecommendedNotifDedupeEpochDay() &&
            sig == VersePreferences.getRecommendedNotifDedupeSignature()
        ) {
            return@withContext Result.success()
        }

        val pendingIntent = pendingIntentForRecommendation(
            applicationContext,
            first,
            Codes.NOTIF_ID_VERSE_REMINDER,
        ) ?: return@withContext Result.success()

        val manager = ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java,
        ) ?: return@withContext Result.success()

        val body = first.description.trim().ifBlank {
            applicationContext.getString(R.string.labelRecommended)
        }

        val notification = NotificationCompat
            .Builder(applicationContext, NotificationUtils.CHANNEL_ID_RECOMMENDED_REMINDER)
            .setContentTitle(first.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.dr_logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(Codes.NOTIF_ID_VERSE_REMINDER, notification)

        VersePreferences.setRecommendedNotifDedupeState(epochDay, sig)

        Result.success()
    }

    private fun dedupeSignature(recommendation: Recommendation): String {
        val ref = when (val r = recommendation.reference) {
            is RecommendationRef.Chapter -> "c:${r.number}"
            is RecommendationRef.Verses -> "v:${r.spec}"
        }

        return "${recommendation.title}\u0000$ref"
    }

    private fun pendingIntentForRecommendation(
        context: Context,
        recommendation: Recommendation,
        requestCode: Int,
    ): PendingIntent? {
        val intent = when (val ref = recommendation.reference) {
            is RecommendationRef.Chapter -> {
                ReaderLaunchParams(
                    data = ReaderIntentData.FullChapter(ref.number),
                ).toIntent().apply {
                    setClass(context, ActivityReader::class.java)
                }
            }

            is RecommendationRef.Verses -> {
                val ranges = ref.spec.split(',')
                val chapters = mutableListOf<Int>()
                val verseSpecs = mutableListOf<String>()

                for (rangeSpec in ranges) {
                    val trimmed = rangeSpec.trim()
                    val chapterNo =
                        trimmed.split(':').firstOrNull()?.toIntOrNull() ?: return null
                    chapters.add(chapterNo)
                    verseSpecs.add(trimmed)
                }

                val desc = recommendation.description.takeIf { it.isNotBlank() }

                ReaderFactory.prepareReferenceVerseIntent(
                    recommendation.title,
                    desc,
                    emptyArray(),
                    chapters,
                    verseSpecs,
                ).apply {
                    setClass(context, ActivityReference::class.java)
                }
            }
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
