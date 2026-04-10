package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.quranapp.android.db.DatabaseProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ReadHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = DatabaseProvider.getUserRepository(application)
    private val quranRepository = DatabaseProvider.getQuranRepository(application)

    val allHistories = userRepository.getHistoriesPaginated()
        .map { pagingData ->
            pagingData.map { item ->
                item.apply {
                    chapterName = quranRepository.getChapterName(item.chapterNo)
                }
            }
        }
        .cachedIn(viewModelScope)

    val recentHistories = userRepository.getHistoriesFlow(10)
        .mapLatest {
            it.forEach { item ->
                item.chapterName = quranRepository.getChapterName(item.chapterNo)
            }

            it
        }
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
