package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookmarksUiState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val chapterNames: Map<Int, String> = emptyMap(),
)

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = DatabaseProvider.getUserRepository(application)
    private val quranRepository = DatabaseProvider.getQuranRepository(application)

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getBookmarksFlow().collectLatest { bookmarks ->
                val chapterNames = loadMissingChapterNames(bookmarks)

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        bookmarks = bookmarks,
                        chapterNames = state.chapterNames + chapterNames,
                    )
                }
            }
        }
    }

    suspend fun removeBookmark(id: Long) {
        userRepository.removeBookmarksBulk(longArrayOf(id))
    }

    suspend fun removeBookmarks(ids: Set<Long>) {
        if (ids.isEmpty()) return
        userRepository.removeBookmarksBulk(ids.toLongArray())
    }

    suspend fun removeAllBookmarks() {
        userRepository.removeAllBookmarks()
    }

    private suspend fun loadMissingChapterNames(
        bookmarks: List<BookmarkEntity>
    ): Map<Int, String> {
        val existing = _uiState.value.chapterNames
        val missing = bookmarks
            .map { it.chapterNo }
            .distinct()
            .filter { it > 0 && !existing.containsKey(it) }

        if (missing.isEmpty()) {
            return emptyMap()
        }

        return withContext(Dispatchers.IO) {
            missing.associateWith { quranRepository.getChapterName(it) }
        }
    }
}
