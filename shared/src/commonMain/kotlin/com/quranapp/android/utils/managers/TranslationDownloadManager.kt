package com.quranapp.android.utils.managers

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.platform.Platform
import com.quranapp.android.task.ExistingTaskPolicy
import com.quranapp.android.task.TaskParams
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskState
import com.quranapp.android.utils.workers.TranslationDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object TranslationDownloadManager {
    private const val TAG = "download_translation"
    private const val ITEM_TAG_PREFIX = "DownloadTranslation:"

    private val downloadStates = MutableLiveData<Map<String, TaskState>>(emptyMap())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    private val taskManager get() = Platform.system.taskManager

    fun initialize() {
        if (observeJob?.isActive == true) return

        observeJob = scope.launch {
            taskManager.infosByTag(TAG).collect { infos ->
                val previous = downloadStates.value.orEmpty()
                val next = mutableMapOf<String, TaskState>()

                for (info in infos) {
                    val slug = info.tags.firstOrNull { it.startsWith(ITEM_TAG_PREFIX) }
                        ?.removePrefix(ITEM_TAG_PREFIX) ?: continue

                    when {
                        !info.state.isFinished -> next[slug] = info.state
                        slug !in next && previous[slug]?.isFinished == false ->
                            next[slug] = info.state
                    }
                }

                downloadStates.postValue(next)
            }
        }
    }

    fun startDownload(bookInfo: TranslationBookInfoModel) {
        val itemTag = "${ITEM_TAG_PREFIX}${bookInfo.slug}"

        taskManager.enqueueUnique(
            uniqueName = itemTag,
            policy = ExistingTaskPolicy.KEEP,
            request = TaskRequest(
                workerClass = TranslationDownloadWorker::class,
                params = TaskParams.of("bookInfo" to Json.encodeToString(bookInfo)),
                tags = setOf(TAG, itemTag),
            ),
        )
    }

    fun stopDownload(slug: String) {
        taskManager.cancelUnique("${ITEM_TAG_PREFIX}$slug")
    }

    private fun removeState(slug: String) {
        val current = downloadStates.value?.toMutableMap() ?: mutableMapOf()
        current.remove(slug)
        downloadStates.postValue(current)
    }

    fun isDownloading(slug: String): Boolean {
        val state = downloadStates.value?.get(slug) ?: return false
        return state.isEnqueued || state.isRunning
    }

    fun isAnyDownloading(): Boolean {
        val currentStates = downloadStates.value ?: return false
        return currentStates.values.any { it.isEnqueued || it.isRunning }
    }

    fun observeDownloadsAsFlow(): Flow<Pair<String, TaskState>> = callbackFlow {
        val observer = Observer<Map<String, TaskState>> { map ->
            for ((slug, state) in map) {
                if (state.isFinished) removeState(slug)
                trySend(slug to state)
            }
        }

        downloadStates.observeForever(observer)

        awaitClose {
            downloadStates.removeObserver(observer)
        }
    }
}
