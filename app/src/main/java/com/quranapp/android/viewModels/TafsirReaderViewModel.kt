package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.tafsir.TafsirModel
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.tafsir.TafsirUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TafsirContentState {
    object Loading : TafsirContentState
    data class Success(val tafsir: TafsirModel) : TafsirContentState
    data class Error(val message: String, val canRetry: Boolean) : TafsirContentState
    object NoInternet : TafsirContentState
}

data class TafsirReaderUiState(
    val tafsirKey: String = "",
    val tafsirInfo: TafsirInfoModel? = null,
    val chapterNo: Int = 0,
    val verseNo: Int = 0,
    val chapterMeta: SurahWithLocalizations? = null,
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
    val repository = DatabaseProvider.getQuranRepository(context)

    private var contentLoadJob: Job? = null

    init {
        viewModelScope.launch {
            ReaderPreferences.tafsirIdFlow()
                .distinctUntilChanged()
                .collectLatest { onEvent(TafsirReaderEvent.ChangeTafsir(it)) }
        }
    }

    fun onEvent(event: TafsirReaderEvent) {
        when (event) {
            is TafsirReaderEvent.Init -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        initialize(event.tafsirKey, event.chapterNo, event.verseNo)
                    }
                }
            }

            is TafsirReaderEvent.ChangeTafsir -> changeTafsir(event.tafsirKey)
            is TafsirReaderEvent.NextVerse -> navigateByTafsirGroup(1)
            is TafsirReaderEvent.PreviousVerse -> navigateByTafsirGroup(-1)
            is TafsirReaderEvent.Retry -> loadTafsirContent()
            is TafsirReaderEvent.UpdateTextSize -> updateTextSize(event.multiplier)
        }
    }

    private suspend fun initialize(tafsirKey: String?, chapterNo: Int, verseNo: Int) {
        val key =
            tafsirKey ?: ReaderPreferences.getTafsirId() ?: TafsirUtils.getDefaultTafsirKey()

        if (key == null) {
            _uiState.update {
                it.copy(contentState = TafsirContentState.Error("No tafsir available", false))
            }
            return
        }

        val tafsirInfo = TafsirManager.getModel(key)
        val chapterMeta = repository.getSurahWithLocalizations(chapterNo)

        _uiState.update {
            it.copy(
                tafsirKey = key,
                tafsirInfo = tafsirInfo,
                chapterNo = chapterNo,
                verseNo = verseNo,
                chapterMeta = chapterMeta,
                textSizeMultiplier = ReaderPreferences.getTafsirTextSizeMultiplier(),
                contentState = TafsirContentState.Loading
            )
        }

        loadTafsirContent()
    }

    private fun changeTafsir(tafsirKey: String) {
        val snapshot = _uiState.value

        if (snapshot.chapterNo >= 1 &&
            snapshot.verseNo >= 1 &&
            snapshot.tafsirKey == tafsirKey
        ) {
            return
        }

        val tafsirInfo = TafsirManager.getModel(tafsirKey)

        if (snapshot.chapterNo < 1 || snapshot.verseNo < 1) {
            _uiState.update {
                it.copy(
                    tafsirKey = tafsirKey,
                    tafsirInfo = tafsirInfo,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                tafsirKey = tafsirKey,
                tafsirInfo = tafsirInfo,
                contentState = TafsirContentState.Loading
            )
        }

        loadTafsirContent()
    }

    /**
     * Moves by whole tafsir blocks: next goes to the first ayah after the current block;
     * previous goes to the first ayah of the block before the current one.
     */
    private fun navigateByTafsirGroup(delta: Int) {
        val state = _uiState.value
        if (state.chapterNo < 1 || state.verseNo < 1) return

        viewModelScope.launch {
            val coord = withContext(Dispatchers.IO) {
                resolveGroupNavigationTarget(state, delta)
            } ?: return@launch

            val newMeta = if (coord.chapterNo != state.chapterNo) {
                withContext(Dispatchers.IO) {
                    repository.getSurahWithLocalizations(coord.chapterNo)
                }
            } else {
                state.chapterMeta
            }

            _uiState.update {
                it.copy(
                    chapterNo = coord.chapterNo,
                    verseNo = coord.verseNo,
                    chapterMeta = newMeta ?: it.chapterMeta,
                    contentState = TafsirContentState.Loading
                )
            }

            loadTafsirContent()
        }
    }

    private suspend fun resolveGroupNavigationTarget(
        state: TafsirReaderUiState,
        delta: Int,
    ): ChapterVersePair? {
        val tafsirKey = state.tafsirKey
        val chapterNo = state.chapterNo
        val verseNo = state.verseNo

        val totalVerses = repository.getSurahWithLocalizations(chapterNo)?.surah?.ayahCount
            ?: return null
        if (totalVerses < 1) return null

        val lastChapter = QuranMeta.chapterRange.last

        val dbHelper = QuranTafsirDBHelper(context)
        try {
            val currentModel: TafsirModel? = when (val c = state.contentState) {
                is TafsirContentState.Success -> c.tafsir
                else -> dbHelper.getTafsirByVerse(tafsirKey, chapterNo, verseNo)
            }

            val range = currentModel?.let { TafsirUtils.tafsirVerseRangeInChapter(it, chapterNo) }
                ?: (verseNo..verseNo)

            return when (delta) {
                1 -> {
                    val next = range.last + 1
                    when {
                        next <= totalVerses -> ChapterVersePair(chapterNo, next)
                        chapterNo < lastChapter -> ChapterVersePair(chapterNo + 1, 1)
                        else -> null
                    }
                }

                -1 -> {
                    val first = range.first

                    if (first > 1) {
                        val beforeCurrentGroup = first - 1
                        ChapterVersePair(chapterNo, beforeCurrentGroup)
                    } else if (chapterNo > 1) {
                        val prevChapter = chapterNo - 1
                        val prevTotal = repository.getSurahWithLocalizations(prevChapter)
                            ?.surah?.ayahCount
                            ?: return null

                        if (prevTotal < 1) return null

                        ChapterVersePair(prevChapter, prevTotal)
                    } else {
                        null
                    }
                }

                else -> null
            }
        } finally {
            dbHelper.close()
        }
    }

    private fun updateTextSize(multiplier: Float) {
        viewModelScope.launch { ReaderPreferences.setTafsirTextSizeMultiplier(multiplier) }
        _uiState.update { it.copy(textSizeMultiplier = multiplier) }
    }

    private fun loadTafsirContent() {
        val state = _uiState.value
        if (state.tafsirKey.isBlank() || state.chapterNo < 1 || state.verseNo < 1) {
            return
        }

        val tafsirKey = state.tafsirKey
        val chapterNo = state.chapterNo
        val verseNo = state.verseNo
        val verseKey = "$chapterNo:$verseNo"

        contentLoadJob?.cancel()
        contentLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(contentState = TafsirContentState.Loading) }

            val outcome = try {
                withContext(Dispatchers.IO) {
                    val dbHelper = QuranTafsirDBHelper(context)

                    try {
                        dbHelper.getTafsirByVerse(tafsirKey, chapterNo, verseNo)?.let {
                            return@withContext LoadOutcome.Success(it)
                        }

                        if (!NetworkStateReceiver.isNetworkConnected(context)) {
                            return@withContext LoadOutcome.NoNetwork
                        }

                        val response = RetrofitInstance.alfaazplus.getTafsirsByVerse(
                            tafsirKey, verseKey
                        )

                        dbHelper.storeTafsirs(
                            response.tafsirs,
                            response.version,
                            response.timestamp1
                        )

                        val match = response.tafsirs.firstOrNull { t ->
                            t.verseKey == verseKey || t.verses.contains(verseKey)
                        }

                        if (match != null) LoadOutcome.Success(match)
                        else LoadOutcome.NotFound
                    } finally {
                        dbHelper.close()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                LoadOutcome.Failed(e.message ?: "Failed to load tafsir")
            }

            when (outcome) {
                is LoadOutcome.Success -> _uiState.update {
                    it.copy(
                        contentState = TafsirContentState.Success(outcome.model)
                    )
                }

                LoadOutcome.NotFound -> _uiState.update {
                    it.copy(contentState = TafsirContentState.Error("Tafsir not found", true))
                }

                LoadOutcome.NoNetwork -> _uiState.update {
                    it.copy(contentState = TafsirContentState.NoInternet)
                }

                is LoadOutcome.Failed -> _uiState.update {
                    it.copy(
                        contentState = TafsirContentState.Error(outcome.message, true)
                    )
                }
            }
        }
    }

    private sealed interface LoadOutcome {
        data class Success(val model: TafsirModel) : LoadOutcome
        object NotFound : LoadOutcome
        object NoNetwork : LoadOutcome
        data class Failed(val message: String) : LoadOutcome
    }
}
