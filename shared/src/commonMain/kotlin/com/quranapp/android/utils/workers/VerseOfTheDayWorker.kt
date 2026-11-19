package com.quranapp.android.utils.workers

import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelVerseSerialWithChapter
import com.quranapp.android.generated.resources.strTitleVOTD
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.notification.AndroidNotificationManager
import com.quranapp.android.platform.notification.AppNotification
import com.quranapp.android.platform.notification.NotificationCategory
import com.quranapp.android.platform.notification.NotificationIntent
import com.quranapp.android.platform.notification.NotificationPriority
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.VerseUtils
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.miscellenous.stripHtmlTags
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.TranslUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class VerseOfTheDayWorker() : TaskWorker {
    companion object {
        private const val NOTIFICATION_ID = 1014
    }

    override suspend fun doWork(context: TaskContext): TaskResult = withContext(Dispatchers.IO) {
        if (!VersePreferences.getVOTDReminderEnabled()) {
            return@withContext TaskResult.Success()
        }

        val repository = DatabaseProvider.getQuranRepository()
        val votd = VerseUtils.getVOTD(repository)
            ?: return@withContext TaskResult.Failure()

        sendNotification(votd)

        return@withContext TaskResult.Success()
    }

    private suspend fun sendNotification(votd: VerseWithDetails) {
        var slugs = ReaderPreferences.getTranslations()
        if (slugs.isEmpty()) {
            slugs = TranslUtils.defaultTranslationSlugs()
        }

        val translationRepo = DatabaseProvider.getTranslationRepository()
        val translations = translationRepo.getTranslations(slugs, votd.chapterNo, votd.verseNo)

        if (translations.isEmpty()) {
            return
        }

        val translationText = translations[0].text.stripHtmlTags(false)

        val verseReference = getString(
            Res.string.strLabelVerseSerialWithChapter,
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
            setClassName("com.quranapp.android", AppDestinations.READER)
        }

        val notification = AppNotification(
            id = NOTIFICATION_ID,
            channelId = AndroidNotificationManager.CHANNEL_ID_VOTD,
            title = getString(Res.string.strTitleVOTD),
            content = translationText,
            subText = verseReference,
            bigText = translationText,
            category = NotificationCategory.REMINDER,
            priority = NotificationPriority.HIGH,
            autoCancel = true,
            contentIntent = NotificationIntent.Platform(readerIntent),
        )

        PlatformServices.system.notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
