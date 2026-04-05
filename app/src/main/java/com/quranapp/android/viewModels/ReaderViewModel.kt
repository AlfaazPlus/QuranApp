package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.mutableStateMapOf
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
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.extensions.asIntRange
import com.quranapp.android.utils.extensions.asPair
import com.quranapp.android.utils.extensions.normalized
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.getQuranMushafId
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
    val currentPageNo: Int? = 1,
    val verseRange: IntRange? = null,
    val transientTranslationSlugs: Set<String>? = null,
    val transientReaderMode: ReaderMode? = null
)

sealed class ReaderIntentData() {
    data class FullJuz(val juzNo: Int) : ReaderIntentData()
    data class FullChapter(val chapterNo: Int) : ReaderIntentData()
    data class VerseRange(val chapterNo: Int, val verseRange: IntRange) : ReaderIntentData()

    var slugs: Set<String>? = null
    var readerMode: ReaderMode? = null
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    val controller = RecitationController.getInstance(application)
    val bookmarksRepository = DatabaseProvider.getUserRepository(application)
    val scriptRepository = DatabaseProvider.getQuranRepository(application)

    val fontResolver = FontResolver.getInstance(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val readerMode = ReaderPreferences.readerModeFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    var autoScrollSpeed = mutableStateOf<Float?>(null)
    var playerVerseSync = mutableStateOf(false)

    private val _verseByVerseItems = MutableStateFlow<List<ReaderLayoutItem>>(emptyList())
    val verseByVerseItems: StateFlow<List<ReaderLayoutItem>> =
        _verseByVerseItems.asStateFlow()

    val pageItems = mutableStateMapOf<Int, QuranPageItem>()
    val pageCounts = mutableStateMapOf<String, Int>()
    private val mushafPagesInFlight = mutableSetOf<Int>()

    /** Script + IndoPak variant; mushaf DB rows change when this changes. */
    private var mushafPageLayoutKey: String? = null

    private val context get() = application

    init {
        controller.connect()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }

    fun updateState(updater: (ReaderUiState) -> ReaderUiState) {
        _uiState.update {
            updater(it)
        }
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
                    ReaderPreferences.KEY_SCRIPT,
                    ReaderPreferences.KEY_SCRIPT_VARIANT,
                    ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
                    ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
                    ReaderPreferences.KEY_TRANSLATIONS,
                    ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
                ),
                readerMode,
            ) { uiState, prefs, readerMode ->
                val script = prefs.get(ReaderPreferences.KEY_SCRIPT)
                val scriptVariantRaw = prefs.get(ReaderPreferences.KEY_SCRIPT_VARIANT)
                Triple(
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
                        script = script,
                        slugs = uiState.transientTranslationSlugs
                            ?: prefs.get(ReaderPreferences.KEY_TRANSLATIONS),
                    ),
                    Triple(readerMode, script, scriptVariantRaw),
                )
            }
                .distinctUntilChanged { prev, next ->
                    prev.second == next.second &&
                            prev.third == next.third &&
                            prev.first.rebuildEquals(next.first)
                }
                .collectLatest { (uiState, params, modeAndScript) ->
                    val (readerMode, script, scriptVariantRaw) = modeAndScript
                    when (readerMode) {
                        ReaderMode.VerseByVerse -> {
                            buildVerseByVerseItems(params, uiState, readerMode)
                        }

                        ReaderMode.Reading -> {
                            val layoutKey = "$script|$scriptVariantRaw"

                            if (mushafPageLayoutKey != layoutKey) {
                                mushafPageLayoutKey = layoutKey
                                pageItems.clear()
                            }
                        }

                        ReaderMode.Translation -> {

                        }

                        else -> {
                            // noop
                        }
                    }
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

    private suspend fun buildVerseByVerseItems(
        params: TextBuilderParams,
        state: ReaderUiState,
        readerMode: ReaderMode,
    ) {
        when {
            state.currentJuzNo != null -> {
                _verseByVerseItems.value = withContext(Dispatchers.IO) {
                    ReaderItemsBuilder.buildJuzVersesForTranslationMode(
                        context,
                        params,
                        scriptRepository,
                        state.currentJuzNo
                    )
                }
            }

            state.currentChapterNo != null -> {
                _verseByVerseItems.value = withContext(Dispatchers.IO) {
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

    suspend fun mushafPageCount(script: String, scriptVariant: QuranScriptVariant?): Int {
        return pageCounts.getOrPut(script + scriptVariant?.value) {
            scriptRepository.getNumberOfPages(script.getQuranMushafId(scriptVariant))
        }
    }

    suspend fun prefetchMushafPages(
        context: Context,
        anchorPages: Collection<Int>,
        totalPages: Int,
        params: PageBuilderParams
    ) = withContext(Dispatchers.IO) {
        if (totalPages <= 0) return@withContext

        val targets = linkedSetOf<Int>()
        for (anchorPage in anchorPages) {
            if (anchorPage !in 1..totalPages) continue

            for (d in -MUSHAF_PREFETCH_RADIUS..MUSHAF_PREFETCH_RADIUS) {
                val page = anchorPage + d
                if (page in 1..totalPages) {
                    targets += page
                }
            }
        }

        if (targets.isEmpty()) return@withContext

        val missing = targets.filter { page ->
            !pageItems.containsKey(page) && mushafPagesInFlight.add(page)
        }
        if (missing.isEmpty()) return@withContext

        try {
            fontResolver.prefetch(ReaderPreferences.getQuranScript(), missing)

            val built = ReaderItemsBuilder.buildMushafPages(
                scriptRepository,
                fontResolver,
                missing,
                params
            )

            for (page in missing) {
                built[page]?.let { pageItems[page] = it }
            }
        } finally {
            mushafPagesInFlight.removeAll(missing.toSet())
        }
    }
}

const val MUSHAF_PREFETCH_RADIUS = 4

private fun ReaderUiState.rebuildEquals(other: ReaderUiState): Boolean =
    currentJuzNo == other.currentJuzNo &&
            currentChapterNo == other.currentChapterNo &&
            verseRange == other.verseRange &&
            transientTranslationSlugs == other.transientTranslationSlugs &&
            error == other.error
