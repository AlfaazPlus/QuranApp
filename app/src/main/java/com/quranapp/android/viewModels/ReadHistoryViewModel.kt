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
class ReadHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = DatabaseProvider.getUserRepository(application)
    private val quranRepository = DatabaseProvider.getQuranRepository(application)

    val chapterNames = appLocaleFlow.mapLatest {
        quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyMap()
    )

    val allHistories = userRepository.getHistoriesPaginated()
        .cachedIn(viewModelScope)

    val recentHistories = userRepository.getHistoriesFlow(10)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    suspend fun deleteHistory(id: Long) {
        userRepository.deleteHistory(id)
    }

    suspend fun deleteAllHistories() {
        userRepository.deleteAllHistories()
    }
}
