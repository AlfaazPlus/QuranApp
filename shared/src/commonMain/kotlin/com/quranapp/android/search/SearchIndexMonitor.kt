package com.quranapp.android.search

import com.quranapp.android.platform.Platform
import kotlinx.coroutines.flow.Flow

/** Emits whether translation search's external-content FTS index is being rebuilt. */
object SearchIndexMonitor {
    fun isIndexingFlow(): Flow<Boolean> =
        Platform.system.taskManager.isRunningByTag(SearchIndexScheduler.INDEXING_TAG)
}
