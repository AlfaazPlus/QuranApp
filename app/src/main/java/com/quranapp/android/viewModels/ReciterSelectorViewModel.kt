package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ReciterSelectorViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager by lazy {
        RecitationModelManager.get(getApplication())
    }


    private val refreshReciters = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val quranReciters: StateFlow<List<RecitationQuranModel>?> = refreshReciters
        .onStart { emit(Unit) }
        .mapLatest {
            modelManager.getAllQuranModel()?.reciters?.orEmpty()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val translationReciters: StateFlow<List<RecitationTranslationModel>?> = refreshReciters
        .onStart { emit(Unit) }
        .mapLatest {
            modelManager.getAllTranslationModel()?.reciters?.orEmpty()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun invalidateReciters() {
        viewModelScope.launch {
            modelManager.forceRefreshQuran = true
            modelManager.forceRefreshTranslation = true
            refreshReciters.emit(Unit)
        }
    }
}
