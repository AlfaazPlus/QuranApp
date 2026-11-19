package com.quranapp.android.search

import com.quranapp.android.db.searchindex.SEARCH_INDEX_CONTENT_VERSION
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.OtherPreferences
import com.quranapp.android.task.ExistingTaskPolicy
import com.quranapp.android.task.TaskBackoffPolicy
import com.quranapp.android.task.TaskManager
import com.quranapp.android.task.TaskParams
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.utils.workers.TranslationSearchIndexWorker
import kotlin.time.Duration.Companion.microseconds

object SearchIndexScheduler {
    private const val UNIQUE_SYNC = "translation_search_index_sync_all"
    const val INDEXING_TAG = "translation_search_indexing"

    private fun workManager(): TaskManager =
        PlatformServices.system.taskManager

    private fun buildRequest(
        mode: String,
        slug: String? = null,
        backoffMs: Long? = null,
    ): TaskRequest {
        val params = if (slug != null) {
            TaskParams.of(
                TranslationSearchIndexWorker.KEY_MODE to mode,
                TranslationSearchIndexWorker.KEY_SLUG to slug,
            )
        } else {
            TaskParams.of(TranslationSearchIndexWorker.KEY_MODE to mode)
        }

        return TaskRequest(
            workerClass = TranslationSearchIndexWorker::class,
            params = params,
            tags = mutableSetOf<String>().apply {
                if (mode != TranslationSearchIndexWorker.MODE_REMOVE) {
                    add(INDEXING_TAG)
                }
            }
        ).apply {
            backoffMs?.let {
                backoffPolicy = TaskBackoffPolicy.LINEAR
                backoffDelay = it.microseconds
            }
        }
    }

    fun enqueueSlug(slug: String) {
        workManager().enqueue(
            buildRequest(
                TranslationSearchIndexWorker.MODE_SLUG,
                slug = slug,
                backoffMs = 30_000L,
            ),
        )
    }

    fun enqueueRemoveSlug(slug: String) {
        workManager().enqueue(
            buildRequest(
                TranslationSearchIndexWorker.MODE_REMOVE,
                slug = slug,
            ),
        )
    }

    fun enqueueSyncAll() {
        workManager().enqueueUnique(
            UNIQUE_SYNC,
            ExistingTaskPolicy.APPEND_OR_REPLACE,
            buildRequest(
                TranslationSearchIndexWorker.MODE_ALL,
                backoffMs = 60_000L,
            ),
        )
    }

    fun scheduleTranslationSearchIndexIfNeeded() {
        val lastSchema = OtherPreferences.getTranslationFtsSchemaVersion()

        if (lastSchema != SEARCH_INDEX_CONTENT_VERSION) {
            enqueueSyncAll()
        }
    }

    suspend fun markIndexUpToDate() {
        OtherPreferences.setTranslationFtsSchemaVersion(SEARCH_INDEX_CONTENT_VERSION)
    }
}
