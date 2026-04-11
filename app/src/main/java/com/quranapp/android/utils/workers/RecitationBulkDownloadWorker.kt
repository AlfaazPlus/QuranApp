package com.quranapp.android.utils.workers

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.mediaplayer.RecitationAudioRepository
import com.quranapp.android.utils.mediaplayer.RecitationBulkDownloadNotificationHelper
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

class RecitationBulkDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val reciterId = inputData.getString(KEY_RECITER_ID)
        val kindName = inputData.getString(KEY_KIND)
        val kind = try {
            RecitationAudioKind.valueOf(kindName ?: "")
        } catch (_: Exception) {
            null
        }

        val urlTemplate = inputData.getString(KEY_URL_TEMPLATE)
        val displayTitle = inputData.getString(KEY_DISPLAY_TITLE).orEmpty().ifBlank { "Recitation" }

        if (reciterId == null || kind == null || urlTemplate == null) {
            return Result.failure(workDataOf(KEY_ERROR to "Invalid data"))
        }

        val modelManager = RecitationModelManager.get(applicationContext)
        val workManager = WorkManager.getInstance(applicationContext)

        val pendingChapters = buildList {
            for (chapterNo in QuranMeta.chapterRange) {
                val audioFile = modelManager.getRecitationAudioFile(reciterId, chapterNo)
                if (audioFile.exists() && audioFile.length() > 0L) continue
                val url = RecitationAudioRepository.prepareAudioUrl(urlTemplate, chapterNo)
                    ?: continue
                add(
                    PendingChapter(
                        chapterNo = chapterNo,
                        url = url,
                        outputPath = audioFile.absolutePath,
                    ),
                )
            }
        }

        val total = pendingChapters.size
        setForeground(
            createEnqueueForegroundInfo(
                reciterId,
                kind,
                displayTitle,
                0,
                total.coerceAtLeast(1),
            ),
        )

        val completed = AtomicInteger(0)
        val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)

        try {
            coroutineScope {
                pendingChapters.map { pending ->
                    async {
                        semaphore.withPermit {
                            currentCoroutineContext().ensureActive()

                            val workName =
                                RecitationAudioDownloadWorker.uniqueWorkName(
                                    reciterId,
                                    pending.chapterNo
                                )

                            workManager.enqueueUniqueWork(
                                workName,
                                ExistingWorkPolicy.KEEP,
                                RecitationAudioDownloadWorker.oneTimeRequest(
                                    url = pending.url,
                                    outputPath = pending.outputPath,
                                    title = displayTitle,
                                    subtitle = applicationContext.getString(
                                        R.string.recitationDownloadChapterSubtitle,
                                        pending.chapterNo,
                                    ),
                                    reciterId = reciterId,
                                    audioKind = kind,
                                    isBulkChild = true,
                                ),
                            )

                            awaitSingleWorkTerminal(workManager, workName)

                            val done = completed.incrementAndGet()
                            setForeground(
                                createEnqueueForegroundInfo(
                                    reciterId,
                                    kind,
                                    displayTitle,
                                    done,
                                    total,
                                ),
                            )
                        }
                    }
                }.awaitAll()
            }

            setForeground(
                createEnqueueForegroundInfo(
                    reciterId,
                    kind,
                    displayTitle,
                    completed.get(),
                    total,
                ),
            )

            RecitationBulkDownloadNotificationHelper.updateOverall(
                applicationContext,
                reciterId,
                kind,
                displayTitle,
            )

            return Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Bulk enqueue failed")))
        }
    }

    private suspend fun awaitSingleWorkTerminal(workManager: WorkManager, workName: String) {
        workManager.getWorkInfosForUniqueWorkLiveData(workName).asFlow()
            .mapNotNull { infos -> infos.firstOrNull() }
            .first { info ->
                when (info.state) {
                    WorkInfo.State.SUCCEEDED,
                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED,
                        -> true

                    else -> false
                }
            }
    }

    private fun createEnqueueForegroundInfo(
        reciterId: String,
        kind: RecitationAudioKind,
        displayTitle: String,
        completedCount: Int,
        chapterTotal: Int,
    ): ForegroundInfo {
        val cancelPi = RecitationBulkDownloadNotificationHelper.createCancelAllBulkPendingIntent(
            ctx,
            reciterId,
            kind,
        )

        val builder =
            NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS).apply {
                setAutoCancel(false)
                setOngoing(true)
                setShowWhen(false)
                setSmallIcon(com.quranapp.android.R.drawable.dr_logo)
                setContentTitle(ctx.getString(R.string.recitationBulkEnqueueNotifTitle))
                setContentText(
                    ctx.getString(
                        R.string.recitationBulkEnqueueNotifText,
                        displayTitle,
                        completedCount,
                        chapterTotal,
                    ),
                )
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                val max = chapterTotal.coerceAtLeast(1)
                setProgress(max, completedCount.coerceAtMost(max), false)
            }


        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            cancelPi,
        )

        return ForegroundInfo(
            id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private data class PendingChapter(
        val chapterNo: Int,
        val url: String,
        val outputPath: String,
    )

    companion object {
        const val TAG = "recitation_bulk_download"

        private const val MAX_PARALLEL_DOWNLOADS = 6

        private const val KEY_RECITER_ID = "bulk_reciter_id"
        private const val KEY_KIND = "bulk_kind"
        private const val KEY_URL_TEMPLATE = "bulk_url_template"
        private const val KEY_DISPLAY_TITLE = "bulk_display_title"
        private const val KEY_ERROR = "error"

        fun uniqueWorkName(reciterId: String, kind: RecitationAudioKind): String {
            return "recitation-bulk:$reciterId:${kind.name}"
        }

        /** Tag for UI / WorkManager observation; not used for chapter child workers. */
        fun reciterTag(reciterId: String, kind: RecitationAudioKind): String {
            return "recitation_bulk_reciter:${kind.name}:$reciterId"
        }

        fun inputData(
            reciterId: String,
            kind: RecitationAudioKind,
            urlTemplate: String,
            displayTitle: String,
        ): Data {
            return workDataOf(
                KEY_RECITER_ID to reciterId,
                KEY_KIND to kind.name,
                KEY_URL_TEMPLATE to urlTemplate,
                KEY_DISPLAY_TITLE to displayTitle,
            )
        }

        fun oneTimeRequest(
            reciterId: String,
            kind: RecitationAudioKind,
            urlTemplate: String,
            displayTitle: String,
        ) = OneTimeWorkRequestBuilder<RecitationBulkDownloadWorker>()
            .setInputData(inputData(reciterId, kind, urlTemplate, displayTitle))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(TAG)
            .addTag(reciterTag(reciterId, kind))
            .build()
    }
}
