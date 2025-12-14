package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.tafsir.TafsirModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.tafsir.TafsirUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TafsirContentState {
    object Loading : TafsirContentState
    data class Success(val tafsir: TafsirModel, val fromCache: Boolean) : TafsirContentState
    data class Error(val message: String, val canRetry: Boolean) : TafsirContentState
    object NoInternet : TafsirContentState
}

data class TafsirReaderUiState(
    val tafsirKey: String = "",
    val tafsirInfo: TafsirInfoModel? = null,
    val chapterNo: Int = 0,
    val verseNo: Int = 0,
    val chapterMeta: QuranMeta.ChapterMeta? = null,
    val contentState: TafsirContentState = TafsirContentState.Loading,
    val textSizeMultiplier: Float = 1.0f
)

sealed interface TafsirReaderEvent {
    data class Init(val tafsirKey: String?, val chapterNo: Int, val verseNo: Int) :
        TafsirReaderEvent

    data class ChangeTafsir(val tafsirKey: String) : TafsirReaderEvent
    object NextVerse : TafsirReaderEvent
    object PreviousVerse : TafsirReaderEvent
    object Retry : TafsirReaderEvent
    data class UpdateTextSize(val multiplier: Float) : TafsirReaderEvent
}

class TafsirReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TafsirReaderUiState())
    val uiState: StateFlow<TafsirReaderUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()
    private var quranMeta: QuranMeta? = null

    fun setQuranMeta(meta: QuranMeta) {
        quranMeta = meta
    }

    fun onEvent(event: TafsirReaderEvent) {
        when (event) {
            is TafsirReaderEvent.Init -> initialize(event.tafsirKey, event.chapterNo, event.verseNo)
            is TafsirReaderEvent.ChangeTafsir -> changeTafsir(event.tafsirKey)
            is TafsirReaderEvent.NextVerse -> navigateVerse(1)
            is TafsirReaderEvent.PreviousVerse -> navigateVerse(-1)
            is TafsirReaderEvent.Retry -> loadTafsirContent()
            is TafsirReaderEvent.UpdateTextSize -> updateTextSize(event.multiplier)
        }
    }

    private fun initialize(tafsirKey: String?, chapterNo: Int, verseNo: Int) {
        val key =
            tafsirKey ?: SPReader.getSavedTafsirKey(context) ?: TafsirUtils.getDefaultTafsirKey()

        if (key == null) {
            _uiState.update {
                it.copy(contentState = TafsirContentState.Error("No tafsir available", false))
            }
            return
        }

        val tafsirInfo = TafsirManager.getModel(key)
        val chapterMeta = quranMeta?.getChapterMeta(chapterNo)

        _uiState.update {
            it.copy(
                tafsirKey = key,
                tafsirInfo = tafsirInfo,
                chapterNo = chapterNo,
                verseNo = verseNo,
                chapterMeta = chapterMeta,
                textSizeMultiplier = SPReader.getSavedTextSizeMultTafsir(context),
                contentState = TafsirContentState.Loading
            )
        }

        loadTafsirContent()
    }

    private fun changeTafsir(tafsirKey: String) {
        val tafsirInfo = TafsirManager.getModel(tafsirKey)

        _uiState.update {
            it.copy(
                tafsirKey = tafsirKey,
                tafsirInfo = tafsirInfo,
                contentState = TafsirContentState.Loading
            )
        }

        loadTafsirContent()
    }

    private fun navigateVerse(delta: Int) {
        val state = _uiState.value
        val newVerseNo = state.verseNo + delta

        if (newVerseNo < 1 || newVerseNo > (state.chapterMeta?.verseCount ?: 0)) return

        _uiState.update {
            it.copy(
                verseNo = newVerseNo,
                contentState = TafsirContentState.Loading
            )
        }

        loadTafsirContent()
    }

    private fun updateTextSize(multiplier: Float) {
        SPReader.setSavedTextSizeMultTafsir(context, multiplier)
        _uiState.update { it.copy(textSizeMultiplier = multiplier) }
    }

    private fun loadTafsirContent() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(contentState = TafsirContentState.Loading) }

            val cachedTafsir = withContext(Dispatchers.IO) {
                QuranTafsirDBHelper(context).use {
                    it.getTafsirByVerse(state.tafsirKey, state.chapterNo, state.verseNo)
                }
            }

            if (cachedTafsir != null) {
                _uiState.update {
                    it.copy(
                        contentState = TafsirContentState.Success(
                            cachedTafsir,
                            fromCache = true
                        )
                    )
                }
                return@launch
            }

            if (!NetworkStateReceiver.isNetworkConnected(context)) {
                _uiState.update { it.copy(contentState = TafsirContentState.NoInternet) }
                return@launch
            }

            try {
                val verseKey = "${state.chapterNo}:${state.verseNo}"
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.alfaazplus.getTafsirsByVerse(state.tafsirKey, verseKey)
                }

                var tafsirToShow: TafsirModel? = null

                withContext(Dispatchers.IO) {
                    QuranTafsirDBHelper(context).use { dbHelper ->
                        dbHelper.storeTafsirs(response.tafsirs)

                       for (tafsir in response.tafsirs) {
                            if (tafsir.verseKey == verseKey || tafsir.verses.contains(verseKey)) {
                                tafsirToShow = tafsir
                            }
                        }
                    }
                }

                if (tafsirToShow != null) {
                    _uiState.update {
                        it.copy(
                            contentState = TafsirContentState.Success(
                                tafsirToShow,
                                fromCache = false
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(contentState = TafsirContentState.Error("Tafsir not found", true))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        contentState = TafsirContentState.Error(
                            e.message ?: "Failed to load tafsir", true
                        )
                    )
                }
            }
        }
    }
}
