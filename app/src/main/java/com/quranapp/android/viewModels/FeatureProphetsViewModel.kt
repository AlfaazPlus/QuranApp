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
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranProphet
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

class FeatureProphetsViewModel : ViewModel() {
    var prophets = listOf<QuranProphet.Prophet>()
        private set
    var quranMeta = MutableStateFlow(QuranMeta())
        private set
    var isLoading by mutableStateOf(false)
    var limit by mutableStateOf(10)

    fun init(context: Context) {
        isLoading = true
        QuranMeta.prepareInstance(context,
            object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    quranMeta.update { r }
                    QuranProphet.prepareInstance(context, quranMeta.value) { quranProphet ->
                        prophets = if (limit > 0) quranProphet.prophets.subList(0, quranProphet.prophets.size.coerceAtMost(limit))
                        else  quranProphet.prophets
                        isLoading = false
                    }
                }
            }
        )
    }



}