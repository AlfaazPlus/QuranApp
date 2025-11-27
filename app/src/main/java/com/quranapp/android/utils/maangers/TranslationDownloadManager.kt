package com.quranapp.android.utils.maangers

import TranslationDownloadWorker
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo
import com.quranapp.android.utils.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object TranslationDownloadManager {
    private const val TAG = "download_translation"
    private const val ITEM_TAG_PREFIX = "DownloadTranslation:"

    private val downloadStates = MutableLiveData<Map<String, WorkInfo>>(emptyMap())

    fun initialize(context: Context) {
        val wm = WorkManager.getInstance(context)

        wm.getWorkInfosByTagLiveData(TAG).observeForever { workInfos ->
            val map = mutableMapOf<String, WorkInfo>()

            for (info in workInfos) {
                if (info.state.isFinished) {
                    continue
                }

                val slugTag = info.tags.firstOrNull { it.startsWith(ITEM_TAG_PREFIX) }
                if (slugTag != null) {
                    val slug = slugTag.substringAfter(ITEM_TAG_PREFIX)
                    map[slug] = info
                }
            }

            Logger.d("TranslationDownloadManager: Current download states: $map")

            downloadStates.postValue(map)
        }
    }

    fun startDownload(context: Context, bookInfo: QuranTranslBookInfo) {
        val data = workDataOf("bookInfo" to Json.encodeToString(bookInfo))
        val itemTag = "${ITEM_TAG_PREFIX}${bookInfo.slug}"

        val request = OneTimeWorkRequestBuilder<TranslationDownloadWorker>()
            .setInputData(data)
            .addTag(TAG)
            .addTag(itemTag)
            .build()

        val wm = WorkManager.getInstance(context)

        wm.enqueueUniqueWork(
            itemTag,
            ExistingWorkPolicy.KEEP,
            request
        )

        observeWork(context, bookInfo.slug, request.id)
    }

    fun stopDownload(context: Context, slug: String) {
        val itemTag = "${ITEM_TAG_PREFIX}$slug"
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(itemTag)
    }

    private fun observeWork(context: Context, slug: String, workId: UUID) {
        val wm = WorkManager.getInstance(context)
        val liveData = wm.getWorkInfoByIdLiveData(workId)

        val observer = object : Observer<WorkInfo?> {
            override fun onChanged(value: WorkInfo?) {
                if (value != null) {
                    updateState(slug, value)

                    if (value.state.isFinished) {
                        liveData.removeObserver(this)
                    }
                }
            }
        }

        liveData.observeForever(observer)
    }

    fun observeDownloads(
        viewLifecycleOwner: LifecycleOwner,
        listener: TranslationDownloadStateListener
    ) {
        downloadStates
            .observe(viewLifecycleOwner) { map ->
                if (map.isEmpty()) {
                    return@observe
                }

                for ((slug, workInfo) in map) {
                    if (workInfo.state.isFinished) {
                        removeState(slug)
                    }

                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            listener.onTranslDownloadStatus(
                                slug,
                                TranslationDownloadStatus.Started
                            )
                        }

                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getInt("progress", 0)
                            listener.onTranslDownloadStatus(
                                slug,
                                TranslationDownloadStatus.InProgress(progress)
                            )
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            listener.onTranslDownloadStatus(
                                slug,
                                TranslationDownloadStatus.Completed
                            )
                        }

                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString("error")
                            listener.onTranslDownloadStatus(
                                slug,
                                TranslationDownloadStatus.Failed(error)
                            )
                        }

                        WorkInfo.State.CANCELLED -> {
                            listener.onTranslDownloadStatus(
                                slug,
                                TranslationDownloadStatus.Cancelled
                            )
                        }

                        else -> {
                            // noop
                        }
                    }
                }
            }
    }

    private fun updateState(slug: String, info: WorkInfo) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current[slug] = info
        downloadStates.postValue(current)
    }

    private fun removeState(slug: String) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current.remove(slug)
        downloadStates.postValue(current)
    }

    fun isDownloading(slug: String): Boolean {
        val currentStates = downloadStates.value ?: return false
        val workInfo = currentStates[slug] ?: return false
        return workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
    }

    fun isAnyDownloading(): Boolean {
        val currentStates = downloadStates.value ?: return false
        return currentStates.values.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }
}


sealed class TranslationDownloadStatus {
    object Started : TranslationDownloadStatus()
    data class InProgress(val progress: Int) : TranslationDownloadStatus()
    object Completed : TranslationDownloadStatus()
    data class Failed(val error: String?) : TranslationDownloadStatus()
    object Cancelled : TranslationDownloadStatus()
}

interface TranslationDownloadStateListener {
    fun onTranslDownloadStatus(slug: String, status: TranslationDownloadStatus)
}
