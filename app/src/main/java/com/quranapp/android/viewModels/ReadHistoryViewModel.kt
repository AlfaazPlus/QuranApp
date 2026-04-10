package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.ReadHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReadHistoryUiState(
    val isLoading: Boolean = true,
    val histories: List<ReadHistoryEntity> = emptyList(),
    val chapterNames: Map<Int, String> = emptyMap(),
)

class ReadHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = DatabaseProvider.getUserRepository(application)
    private val quranRepository = DatabaseProvider.getQuranRepository(application)

    private val _uiState = MutableStateFlow(ReadHistoryUiState())
    val uiState: StateFlow<ReadHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllHistoriesFlow().collectLatest { histories ->
                val chapterNames = loadMissingChapterNames(histories)

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        histories = histories,
                        chapterNames = state.chapterNames + chapterNames,
                    )
                }
            }
        }
    }

    suspend fun deleteHistory(id: Long) {
        userRepository.deleteHistory(id)
    }

    suspend fun deleteAllHistories() {
        userRepository.deleteAllHistories()
    }

    private suspend fun loadMissingChapterNames(
        histories: List<ReadHistoryEntity>
    ): Map<Int, String> {
        val existing = _uiState.value.chapterNames
        val missing = histories
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
