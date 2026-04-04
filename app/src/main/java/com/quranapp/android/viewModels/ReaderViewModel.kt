package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.bookmark.BookmarkDatabaseProvider
import com.quranapp.android.utils.extensions.asIntRange
import com.quranapp.android.utils.extensions.asPair
import com.quranapp.android.utils.extensions.normalized
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.VerseActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    var error: String? = null,
    val currentChapterNo: Int? = null,
    val currentJuzNo: Int? = null,
    val verseRange: IntRange? = null,
    val transientTranslationSlugs: Set<String>? = null,
    val transientReaderMode: Int? = null
)

sealed class ReaderIntentData() {
    data class FullJuz(val juzNo: Int) : ReaderIntentData()
    data class FullChapter(val chapterNo: Int) : ReaderIntentData()
    data class VerseRange(val chapterNo: Int, val verseRange: IntRange) : ReaderIntentData()

    var slugs: Set<String>? = null
    var readerMode: Int? = null
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    val controller = RecitationController.getInstance(application)
    val bookmarksRepository = BookmarkDatabaseProvider.getRepository(application)
    private val fontResolver = FontResolver(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val readerMode = ReaderPreferences.readerStyleFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    var autoScrollSpeed = mutableStateOf<Float?>(null)
    var playerVerseSync = mutableStateOf(false)

    private val _translationViewItems = MutableStateFlow<List<ReaderLayoutItem>>(emptyList())
    val translationViewItems: StateFlow<List<ReaderLayoutItem>> =
        _translationViewItems.asStateFlow()

    val pageViewItems = mutableStateOf<List<QuranPageItem>>(emptyList())

    private val context get() = application

    init {
        controller.connect()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }

    fun observeChanges(
        context: Context,
        colors: ColorScheme,
        type: Typography,
        verseActions: VerseActions
    ) {
        viewModelScope.launch {
            combine(
                _uiState,
                DataStoreManager.flowMultiple(
                    ReaderPreferences.KEY_READER_STYLE,
                    ReaderPreferences.KEY_SCRIPT,
                    ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
                    ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
                    ReaderPreferences.KEY_TRANSLATIONS,
                    ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
                ),
            ) { uiState, prefs ->
                Pair(
                    uiState,
                    TextBuilderParams(
                        context,
                        fontResolver,
                        verseActions,
                        colors,
                        type,
                        arabicEnabled = prefs.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED),
                        arabicSizeMultiplier = prefs.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC),
                        translationSizeMultiplier = prefs.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL),
                        script = prefs.get(ReaderPreferences.KEY_SCRIPT),
                        slugs = uiState.transientTranslationSlugs
                            ?: prefs.get(ReaderPreferences.KEY_TRANSLATIONS),
                    ),
                )
            }
                .distinctUntilChanged { prev, next ->
                    prev.second == next.second && prev.first.rebuildEquals(next.first)
                }
                .collectLatest { (uiState, params) ->
                    buildItems(params, uiState)
                }
        }
    }

    suspend fun initReader(data: ReaderIntentData) {
        playerVerseSync.value = true

        var state = ReaderUiState(
            transientTranslationSlugs = data.slugs,
            transientReaderMode = data.readerMode,
        )

        val meta = QuranMeta2.prepareInstance(context)

        when (data) {
            is ReaderIntentData.FullJuz -> {
                if (QuranMeta.isJuzValid(data.juzNo)) {
                    state = state.copy(
                        currentJuzNo = data.juzNo,
                    )
                } else {
                    state = state.copy(
                        error = context.getString(R.string.strMsgInvalidJuzNo, data.juzNo)
                    )
                }
            }

            is ReaderIntentData.FullChapter -> {
                if (QuranMeta.isChapterValid(data.chapterNo)) {
                    state = state.copy(
                        currentChapterNo = data.chapterNo,
                    )
                } else {
                    state = state.copy(
                        error = context.getString(R.string.strMsgInvalidChapterNo, data.chapterNo)
                    )
                }
            }

            is ReaderIntentData.VerseRange -> {
                val chapterNo = data.chapterNo
                val verseRange = data.verseRange.normalized

                if (QuranMeta.isChapterValid(data.chapterNo)) {
                    if (
                        QuranUtils.doesRangeDenoteSingle(verseRange.asPair) &&
                        !meta.isVerseValid4Chapter(chapterNo, verseRange.first)
                    ) {
                        state = state.copy(
                            error = context.getString(
                                R.string.strMsgInvalidVerseNo,
                                verseRange.first,
                                chapterNo
                            )
                        )
                    } else {
                        state = state.copy(
                            currentChapterNo = chapterNo,
                            verseRange = if (
                                meta.isVerseRangeValid4Chapter(
                                    chapterNo,
                                    verseRange.asPair
                                )
                            ) {
                                QuranUtils.correctVerseInRange(
                                    meta,
                                    chapterNo,
                                    verseRange.asPair
                                ).asIntRange
                            } else verseRange
                        )
                    }
                } else {
                    state = state.copy(
                        error = context.getString(R.string.strMsgInvalidChapterNo, data.chapterNo)
                    )
                }
            }
        }

        _uiState.update {
            state
        }
    }

    private suspend fun buildItems(
        params: TextBuilderParams,
        state: ReaderUiState,
    ) {
        when {
            state.currentJuzNo != null -> {
                _translationViewItems.value = withContext(Dispatchers.IO) {
                    ReaderItemsBuilder.buildJuzVersesForTranslationMode(
                        context,
                        params,
                        state.currentJuzNo
                    )
                }
            }

            state.currentChapterNo != null -> {
                _translationViewItems.value = withContext(Dispatchers.IO) {
                    ReaderItemsBuilder.buildVersesForTranslationMode(
                        context,
                        params,
                        state.currentChapterNo,
                        state.verseRange?.first,
                        state.verseRange?.last,
                    )
                }
            }
        }

    }
}

private fun ReaderUiState.rebuildEquals(other: ReaderUiState): Boolean =
    currentJuzNo == other.currentJuzNo &&
            currentChapterNo == other.currentChapterNo &&
            verseRange == other.verseRange &&
            transientTranslationSlugs == other.transientTranslationSlugs &&
            error == other.error
