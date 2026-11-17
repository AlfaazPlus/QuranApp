package com.quranapp.android.db.search

import android.content.Context
import com.quranapp.android.components.search.SearchHistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

data class SearchHistoryEntry(
    val id: Int,
    val text: String,
    val date: String,
)

class SearchHistoryStore(context: Context) : Closeable {
    private val appContext = context.applicationContext

    private val helper: SearchHistoryDBHelper by lazy { SearchHistoryDBHelper(appContext) }

    override fun close() {
        helper.close()
    }

    suspend fun loadAll(): List<SearchHistoryEntry> = withContext(Dispatchers.IO) {
        helper.getHistories("").mapNotNull { model ->
            (model as? SearchHistoryModel)?.let {
                SearchHistoryEntry(it.id, it.text.toString(), it.date)
            }
        }
    }

    suspend fun add(trimmedQuery: String) = withContext(Dispatchers.IO) {
        if (trimmedQuery.isBlank()) return@withContext
        helper.addToHistory(trimmedQuery, null)
    }

    suspend fun remove(id: Int) = withContext(Dispatchers.IO) {
        helper.removeFromHistory(id, null)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        helper.clearHistories()
    }
}
