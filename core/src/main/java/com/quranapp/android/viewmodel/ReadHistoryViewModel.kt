package com.quranapp.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.locale.appLocaleFlow
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ReadHistoryViewModel(application: Application) : AppViewModel(application) {
    private val userRepository get() = DatabaseProvider.getUserRepository(application)
    private val quranRepository get() = DatabaseProvider.getQuranRepository(application)

    val chapterNames = appLocaleFlow.mapLatest {
        quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyMap()
    )

    val mainHistory = userRepository.getMainHistoryFlow()
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val allHistories = userRepository.getHistoriesPaginated()
        .cachedIn(viewModelScope)

    val recentHistories = userRepository.getHistoriesFlow(4)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    suspend fun setMainHistory(id: Long) {
        userRepository.setMainHistory(id)
    }

    suspend fun deleteHistory(id: Long) {
        userRepository.deleteHistory(id)
    }

    suspend fun deleteAllHistories() {
        userRepository.deleteAllHistories()
    }
}
