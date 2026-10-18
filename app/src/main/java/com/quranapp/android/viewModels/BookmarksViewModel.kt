package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.compose.utils.appLocaleFlow
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookmarksUiState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = DatabaseProvider.getUserRepository(application)
    private val quranRepository = DatabaseProvider.getQuranRepository(application)

    val chapterNames = appLocaleFlow.mapLatest {
        quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyMap()
    )

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getBookmarksFlow().collectLatest { bookmarks ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        bookmarks = bookmarks,
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
}
