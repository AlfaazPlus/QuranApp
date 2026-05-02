package com.quranapp.android.utils.reader.atlas

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.workers.AtlasDownloadWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

object AtlasDownloadManager {
    private const val TAG = "download_script_atlas"
    private const val ITEM_TAG_PREFIX = "DownloadAtlas:"
    private const val KEY_PROGRESS = "progress"

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

    fun startDownload(context: Context, scriptKey: String, densityLevel: Int) {
        val data = workDataOf(
            "scriptKey" to scriptKey,
            "densityLevel" to densityLevel,
        )

        val itemTag = "${ITEM_TAG_PREFIX}$scriptKey"

        val request = OneTimeWorkRequestBuilder<AtlasDownloadWorker>()
            .setInputData(data)
            .addTag(TAG)
            .addTag(itemTag)
            .build()

        val wm = WorkManager.getInstance(context)

        wm.enqueueUniqueWork(
            itemTag,
            ExistingWorkPolicy.REPLACE,
            request,
        )

        observeWork(context, scriptKey, request.id)
    }

    fun stopDownload(context: Context, scriptKey: String) {
        val itemTag = "${ITEM_TAG_PREFIX}$scriptKey"
        WorkManager.getInstance(context).cancelUniqueWork(itemTag)
    }

    private fun observeWork(context: Context, script: String, workId: UUID) {
        val wm = WorkManager.getInstance(context)
        val liveData = wm.getWorkInfoByIdLiveData(workId)

        val observer = object : Observer<WorkInfo?> {
            override fun onChanged(value: WorkInfo?) {
                if (value != null) {
                    updateState(script, value)

                    if (value.state.isFinished) {
                        liveData.removeObserver(this)
                    }
                }
            }
        }

        liveData.observeForever(observer)
    }

    private fun updateState(script: String, info: WorkInfo) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()

        current[script] = info

        downloadStates.postValue(current)
    }

    private fun removeState(scriptKey: String) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current.remove(scriptKey)
        downloadStates.postValue(current)
    }

    fun observeDownloadsAsFlow(): Flow<Pair<String, ResourceDownloadStatus>> = callbackFlow {
        val observer = Observer<Map<String, WorkInfo>> { map ->
            for ((scriptKey, workInfo) in map) {
                val status = when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> ResourceDownloadStatus.Started
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(KEY_PROGRESS, 0)
                        ResourceDownloadStatus.InProgress(progress)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        removeState(scriptKey)
                        ResourceDownloadStatus.Completed
                    }

                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("error")
                        removeState(scriptKey)
                        ResourceDownloadStatus.Failed(error)
                    }

                    WorkInfo.State.CANCELLED -> {
                        removeState(scriptKey)
                        ResourceDownloadStatus.Cancelled
                    }

                    else -> null
                }

                if (status != null) {
                    trySend(scriptKey to status)
                }
            }
        }

        downloadStates.observeForever(observer)

        awaitClose {
            downloadStates.removeObserver(observer)
        }
    }
}
