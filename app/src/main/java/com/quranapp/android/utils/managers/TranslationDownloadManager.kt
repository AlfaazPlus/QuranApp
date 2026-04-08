package com.quranapp.android.utils.managers

import TranslationDownloadWorker
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

            downloadStates.postValue(map)
        }
    }

    fun startDownload(context: Context, bookInfo: TranslationBookInfoModel) {
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


    fun observeDownloadsAsFlow(): Flow<Pair<String, ResourceDownloadStatus>> = callbackFlow {
        val observer = Observer<Map<String, WorkInfo>> { map ->
            for ((slug, workInfo) in map) {
                val status = when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> ResourceDownloadStatus.Started
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", 0)
                        ResourceDownloadStatus.InProgress(progress)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        removeState(slug)
                        ResourceDownloadStatus.Completed
                    }

                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("error")
                        removeState(slug)
                        ResourceDownloadStatus.Failed(error)
                    }

                    WorkInfo.State.CANCELLED -> {
                        removeState(slug)
                        ResourceDownloadStatus.Cancelled
                    }

                    else -> null
                }

                if (status != null) {
                    trySend(slug to status)
                }
            }
        }

        downloadStates.observeForever(observer)

        awaitClose {
            downloadStates.removeObserver(observer)
        }
    }
}


sealed class ResourceDownloadStatus {
    object Idle : ResourceDownloadStatus()
    object Started : ResourceDownloadStatus()
    data class InProgress(val progress: Int) : ResourceDownloadStatus()
    object Completed : ResourceDownloadStatus()
    data class Failed(val error: String?) : ResourceDownloadStatus()
    object Cancelled : ResourceDownloadStatus()
}