package com.quranapp.android.utils.reader.wbw

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranapp.android.api.models.wbw.WbwLanguageInfo
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.workers.WbwDownloadWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.collections.iterator

object WbwDownloadManager {
    private const val TAG = "download_wbw"
    private const val ITEM_TAG_PREFIX = "DownloadWbw:"

    private val downloadStates = MutableLiveData<Map<String, WorkInfo>>(emptyMap())

    fun initialize(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.getWorkInfosByTagLiveData(TAG).observeForever { workInfos ->
            val map = mutableMapOf<String, WorkInfo>()
            for (info in workInfos) {
                if (info.state.isFinished) continue
                val idTag = info.tags.firstOrNull { it.startsWith(ITEM_TAG_PREFIX) } ?: continue
                val id = idTag.substringAfter(ITEM_TAG_PREFIX)
                map[id] = info
            }
            downloadStates.postValue(map)
        }
    }

    fun startDownload(context: Context, info: WbwLanguageInfo) {
        val data = workDataOf("wbwInfo" to Json.Default.encodeToString(info))
        val itemTag = "${ITEM_TAG_PREFIX}${info.id}"

        val request = OneTimeWorkRequestBuilder<WbwDownloadWorker>()
            .setInputData(data)
            .addTag(TAG)
            .addTag(itemTag)
            .build()

        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(
            itemTag,
            ExistingWorkPolicy.REPLACE,
            request
        )

        observeWork(context, info.id, request.id)
    }

    fun stopDownload(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork("${ITEM_TAG_PREFIX}$id")
    }

    private fun observeWork(context: Context, id: String, workId: UUID) {
        val wm = WorkManager.getInstance(context)
        val liveData = wm.getWorkInfoByIdLiveData(workId)
        val observer = object : Observer<WorkInfo?> {
            override fun onChanged(value: WorkInfo?) {
                if (value != null) {
                    updateState(id, value)
                    if (value.state.isFinished) {
                        liveData.removeObserver(this)
                    }
                }
            }
        }
        liveData.observeForever(observer)
    }

    private fun updateState(id: String, info: WorkInfo) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current[id] = info
        downloadStates.postValue(current)
    }

    private fun removeState(id: String) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current.remove(id)
        downloadStates.postValue(current)
    }

    fun observeDownloadsAsFlow(): Flow<Pair<String, ResourceDownloadStatus>> = callbackFlow {
        val observer = Observer<Map<String, WorkInfo>> { map ->
            for ((id, workInfo) in map) {
                val status = when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> ResourceDownloadStatus.Started
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", 0)
                        ResourceDownloadStatus.InProgress(progress)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        removeState(id)
                        ResourceDownloadStatus.Completed
                    }

                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("error")
                        removeState(id)
                        ResourceDownloadStatus.Failed(error)
                    }

                    WorkInfo.State.CANCELLED -> {
                        removeState(id)
                        ResourceDownloadStatus.Cancelled
                    }

                    else -> null
                }

                if (status != null) {
                    trySend(id to status)
                }
            }
        }

        downloadStates.observeForever(observer)
        awaitClose { downloadStates.removeObserver(observer) }
    }
}
