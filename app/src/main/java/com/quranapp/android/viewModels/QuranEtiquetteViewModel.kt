package com.quranapp.android.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.components.FeaturedQuranModel
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.QuranDua
import com.quranapp.android.components.quran.QuranEtiquette
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.SituationVerse
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
import java.util.Locale

class QuranEtiquetteViewModel : ViewModel() {
    var references = listOf<ExclusiveVerse>()
        private set
    var quranMeta = MutableStateFlow(QuranMeta())
        private set
    var isLoading by mutableStateOf(false)

    fun init(context: Context) {
        isLoading = true
        QuranMeta.prepareInstance(context,
            object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    quranMeta.update { r }
                    QuranEtiquette.prepareInstance(context, quranMeta.value) { referencesList ->
                        references = referencesList.subList(0, referencesList.size.coerceAtMost(5))
                        isLoading = false
                    }
                }
            }
        )
    }



}