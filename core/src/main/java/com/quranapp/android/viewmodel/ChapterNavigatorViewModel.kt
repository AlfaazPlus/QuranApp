package com.quranapp.android.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ChapterNavigatorViewModel(application: Application) :
    AppViewModel(application) {
    val repository get() = DatabaseProvider.getQuranRepository(application)

    val surahs = repository.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
