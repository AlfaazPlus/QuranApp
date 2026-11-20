package com.quranapp.android.utils.managers

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.task.ExistingTaskPolicy
import com.quranapp.android.task.TaskParams
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskState
import com.quranapp.android.utils.workers.ScriptFontsDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.collections.iterator

object ScriptFontsDownloadManager {
    private const val TAG = "download_script_font"
    private const val ITEM_TAG_PREFIX = "DownloadScriptFont:"

    private val downloadStates = MutableLiveData<Map<String, TaskState>>(emptyMap())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    private val taskManager get() = PlatformServices.system.taskManager

    fun initialize() {
        if (observeJob?.isActive == true) return
        observeJob = scope.launch {
            taskManager.infosByTag(TAG).collect { infos ->
                val previous = downloadStates.value.orEmpty()
                val next = mutableMapOf<String, TaskState>()

                for (info in infos) {
                    val key = info.tags.firstOrNull { it.startsWith(ITEM_TAG_PREFIX) }
                        ?.removePrefix(ITEM_TAG_PREFIX) ?: continue

                    when {
                        !info.state.isFinished -> next[key] = info.state
                        key !in next && previous[key]?.isFinished == false ->
                            next[key] = info.state
                    }
                }

                downloadStates.postValue(next)
            }
        }
    }

    fun startDownload(script: String) {
        val itemTag = "${ITEM_TAG_PREFIX}$script"
        taskManager.enqueueUnique(
            uniqueName = itemTag,
            policy = ExistingTaskPolicy.KEEP,
            request = TaskRequest(
                workerClass = ScriptFontsDownloadWorker::class,
                params = TaskParams.of(ScriptFontsDownloadWorker.KEY_SCRIPT to script),
                tags = setOf(TAG, itemTag),
            ),
        )
    }

    fun stopDownload(slug: String) {
        taskManager.cancelUnique("${ITEM_TAG_PREFIX}$slug")
    }

    private fun removeState(script: String) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current.remove(script)
        downloadStates.postValue(current)
    }

    fun observeDownloadsAsFlow(): Flow<Pair<String, TaskState>> = callbackFlow {
        val observer = Observer<Map<String, TaskState>> { map ->
            for ((script, state) in map) {
                if (state.isFinished) removeState(script)
                trySend(script to state)
            }
        }

        downloadStates.observeForever(observer)

        awaitClose {
            downloadStates.removeObserver(observer)
        }
    }
}
