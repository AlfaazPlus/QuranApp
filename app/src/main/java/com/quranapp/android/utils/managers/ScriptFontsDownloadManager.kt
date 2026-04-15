package com.quranapp.android.utils.managers

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranapp.android.utils.workers.ScriptFontsDownloadWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

object ScriptFontsDownloadManager {
    private const val TAG = "download_script_font"
    private const val ITEM_TAG_PREFIX = "DownloadScriptFont:"

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

    fun startDownload(context: Context, script: String) {
        val data = workDataOf(ScriptFontsDownloadWorker.KEY_SCRIPT to script)
        val itemTag = "${ITEM_TAG_PREFIX}${script}"

        val request = OneTimeWorkRequestBuilder<ScriptFontsDownloadWorker>()
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

        observeWork(context, script, request.id)
    }

    fun stopDownload(context: Context, slug: String) {
        val itemTag = "${ITEM_TAG_PREFIX}$slug"
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(itemTag)
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

    private fun removeState(tafsirKey: String) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current.remove(tafsirKey)
        downloadStates.postValue(current)
    }

    fun observeDownloadsAsFlow(): Flow<Pair<String, ResourceDownloadStatus>> = callbackFlow {
        val observer = Observer<Map<String, WorkInfo>> { map ->
            for ((tafsirKey, workInfo) in map) {
                val status = when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> ResourceDownloadStatus.Started
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(
                            ScriptFontsDownloadWorker.KEY_PROGRESS,
                            0
                        )
                        ResourceDownloadStatus.InProgress(progress)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        removeState(tafsirKey)
                        ResourceDownloadStatus.Completed
                    }

                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("error")
                        removeState(tafsirKey)
                        ResourceDownloadStatus.Failed(error)
                    }

                    WorkInfo.State.CANCELLED -> {
                        removeState(tafsirKey)
                        ResourceDownloadStatus.Cancelled
                    }

                    else -> null
                }

                if (status != null) {
                    trySend(tafsirKey to status)
                }
            }
        }

        downloadStates.observeForever(observer)

        awaitClose {
            downloadStates.removeObserver(observer)
        }
    }
}