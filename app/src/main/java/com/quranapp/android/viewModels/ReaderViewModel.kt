package com.quranapp.android.viewModels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.extensions.asIntRange
import com.quranapp.android.utils.extensions.asPair
import com.quranapp.android.utils.extensions.normalized
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ReaderUiState(
    val loading: Boolean = true,
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
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val readerMode = ReaderPreferences.readerStyleFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val slugs = ReaderPreferences.translationsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val translationViewItems = mutableStateOf<List<ReaderLayoutItem>>(emptyList())
    val pageViewItems = mutableStateOf<List<QuranPageItem>>(emptyList())

    private val context get() = application

    suspend fun initReader(data: ReaderIntentData) {
        _uiState.update {
            ReaderUiState(
                loading = true
            )
        }

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

        buildItems(state)
    }

    private suspend fun buildItems(state: ReaderUiState) {
        if (state.currentJuzNo != null) {
            translationViewItems.value = ReaderItemsBuilder.buildJuzVersesForTranslationMode(
                context,
                state.currentJuzNo
            )
        } else if (state.currentChapterNo != null) {
            translationViewItems.value = ReaderItemsBuilder.buildVersesForTranslationMode(
                context,
                state.currentChapterNo,
                state.verseRange?.first,
                state.verseRange?.last,
            )
        }

        _uiState.update { it.copy(loading = false) }
    }
}
