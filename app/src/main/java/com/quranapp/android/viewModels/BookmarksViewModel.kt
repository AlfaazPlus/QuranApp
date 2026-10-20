package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.quranapp.android.compose.utils.appLocaleFlow
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModel(private val application: Application) : AndroidViewModel(application) {
    private val userRepository get() = DatabaseProvider.getUserRepository(application)
    private val quranRepository get() = DatabaseProvider.getQuranRepository(application)

    val chapterNames = appLocaleFlow.mapLatest {
        quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyMap()
    )

    val allBookmarks = userRepository.getBookmarksPagingFlow()
        .cachedIn(viewModelScope)

    val recentBookmarks = userRepository.getBookmarksFlow(4)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

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
