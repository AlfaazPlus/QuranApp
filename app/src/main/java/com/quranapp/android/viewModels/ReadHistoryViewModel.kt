package com.quranapp.android.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.components.readHistory.mapToEntity
import com.quranapp.android.db.entities.ReadHistory
import com.quranapp.android.db.entities.mapToUiModel
import com.quranapp.android.db.readHistory.ReadHistoryDBHolder
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.univ.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadHistoryViewModel : ViewModel() {
    val history: StateFlow<List<ReadHistory>> =
        ReadHistoryDBHolder.instance.readHistoryDao().getAllFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = listOf()
        )
    var quranMeta = MutableStateFlow(QuranMeta())
        private set
    var isLoading by mutableStateOf(false)

    fun init(context: Context) {
        isLoading = true
        QuranMeta.prepareInstance(context,
            object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    quranMeta.update { r }
                    isLoading = false
                }
            }
        )
    }

    fun getAllHistory(): List<ReadHistoryModel> {
        return history.value.map { it.mapToUiModel() }
    }

    fun addToHistoryItem(readType: Int, readerStyle: Int, juzNo: Int,
        chapterNo: Int, fromVerse: Int, toVerse: Int
    ) {
        val historyItem = ReadHistory(readType = readType, readStyle = readerStyle, juzNo = juzNo,
            chapterNo = chapterNo, fromVerseNo = fromVerse, toVerseNo = toVerse, date = DateUtils.dateTimeNow)

        viewModelScope.launch(Dispatchers.IO) {
            ReadHistoryDBHolder.instance.readHistoryDao()
                .deleteDuplicate(readType, readerStyle, juzNo, chapterNo, fromVerse, toVerse)
            ReadHistoryDBHolder.instance.readHistoryDao().insert(historyItem)
        }
    }

    fun deleteHistoryItem(historyItem: ReadHistoryModel) {
        viewModelScope.launch(Dispatchers.IO) {
            ReadHistoryDBHolder.instance.readHistoryDao().delete(historyItem.mapToEntity())
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            ReadHistoryDBHolder.instance.readHistoryDao().deleteAll()
        }

    }


}