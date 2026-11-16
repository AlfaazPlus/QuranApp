package com.quranapp.android.utils.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.core.R
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.locale.formatStringResource
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.miscellenous.StringUtils
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.verse.VerseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerseOfTheDayWorker constructor(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        private const val NOTIFICATION_ID = 1014
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!VersePreferences.getVOTDReminderEnabled()) {
            return@withContext Result.success()
        }

        val repository = DatabaseProvider.getQuranRepository(applicationContext)
        val votd = VerseUtils.getVOTD(applicationContext, repository)
            ?: return@withContext Result.failure()

        sendNotification(votd)

        return@withContext Result.success()
    }

    private suspend fun sendNotification(votd: VerseWithDetails) {
        val context = applicationContext

        var slugs = ReaderPreferences.getTranslations()
        if (slugs.isEmpty()) {
            slugs = TranslUtils.defaultTranslationSlugs()
        }

        val translationRepo = DatabaseProvider.getTranslationRepository(context)
        val translations = translationRepo.getTranslations(slugs, votd.chapterNo, votd.verseNo)

        if (translations.isEmpty()) {
            return
        }

        val manager = ContextCompat.getSystemService(
            context, NotificationManager::class.java
        ) ?: return

        val translationText = StringUtils.removeHTML(translations[0].text, false)

        val verseReference = formatStringResource(
            context,
            R.string.strLabelVerseSerialWithChapter,
            votd.chapter.getCurrentName(),
            votd.chapterNo,
            votd.verseNo
        )

        val readerIntent = ReaderLaunchParams(
            data = ReaderIntentData.FullChapter(
                votd.chapterNo,
                ChapterVersePair(votd.chapterNo, votd.verseNo),
            ),
            slugs = slugs,
        ).toIntent().apply {
            setClassName(context, AppDestinations.READER)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            readerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(applicationContext, NotificationUtils.CHANNEL_ID_VOTD)
            .setContentTitle(context.getString(R.string.strTitleVOTD))
            .setContentText(translationText)
            .setStyle(
                NotificationCompat
                    .BigTextStyle()
                    .bigText(translationText)
            )
            .setSubText(verseReference)
            .setSmallIcon(R.drawable.dr_logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
