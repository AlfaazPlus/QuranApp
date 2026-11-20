package com.quranapp.android.search

import com.quranapp.android.platform.PlatformServices
import kotlinx.coroutines.flow.Flow

/** Emits whether translation search's external-content FTS index is being rebuilt. */
object SearchIndexMonitor {
    fun isIndexingFlow(): Flow<Boolean> =
        PlatformServices.system.taskManager.isRunningByTag(SearchIndexScheduler.INDEXING_TAG)
}
