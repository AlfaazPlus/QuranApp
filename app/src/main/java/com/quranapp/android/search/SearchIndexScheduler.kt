package com.quranapp.android.search

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranapp.android.BuildConfig
import com.quranapp.android.db.searchindex.SEARCH_INDEX_DB_VERSION
import com.quranapp.android.utils.workers.TranslationSearchIndexWorker
import java.util.concurrent.TimeUnit

object SearchIndexScheduler {
    private const val UNIQUE_SYNC = "translation_search_index_sync_all"

    private fun workManager(context: Context): WorkManager =
        WorkManager.getInstance(context.applicationContext)

    private fun buildRequest(
        mode: String,
        slug: String? = null,
        backoffMs: Long? = null,
    ): OneTimeWorkRequest {
        val data = if (slug != null) {
            workDataOf(
                TranslationSearchIndexWorker.KEY_MODE to mode,
                TranslationSearchIndexWorker.KEY_SLUG to slug,
            )
        } else {
            workDataOf(TranslationSearchIndexWorker.KEY_MODE to mode)
        }

        return OneTimeWorkRequestBuilder<TranslationSearchIndexWorker>()
            .setInputData(data)
            .apply {
                backoffMs?.let {
                    setBackoffCriteria(BackoffPolicy.LINEAR, it, TimeUnit.MILLISECONDS)
                }

                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
            .build()
    }

    fun enqueueSlug(context: Context, slug: String) {
        workManager(context).enqueue(
            buildRequest(
                TranslationSearchIndexWorker.MODE_SLUG,
                slug = slug,
                backoffMs = 30_000L,
            ),
        )
    }

    fun enqueueRemoveSlug(context: Context, slug: String) {
        workManager(context).enqueue(
            buildRequest(
                TranslationSearchIndexWorker.MODE_REMOVE,
                slug = slug,
            ),
        )
    }

    fun enqueueSyncAll(context: Context) {
        workManager(context).enqueueUniqueWork(
            UNIQUE_SYNC,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            buildRequest(
                TranslationSearchIndexWorker.MODE_ALL,
                backoffMs = 60_000L,
            ),
        )
    }


    fun scheduleTranslationSearchIndexIfNeeded(context: Context) {
        val sp = context.getSharedPreferences("search_index_prefs", MODE_PRIVATE)
        val lastSchema = sp.getInt("translation_fts_schema", 0)
        val lastVc = sp.getInt("translation_fts_vc", 0)

        if (lastSchema != SEARCH_INDEX_DB_VERSION || lastVc != BuildConfig.VERSION_CODE) {
            sp.edit {
                putInt(
                    "translation_fts_schema",
                    SEARCH_INDEX_DB_VERSION
                )

                putInt("translation_fts_vc", BuildConfig.VERSION_CODE)
            }

            enqueueSyncAll(context)
        }
    }
}
