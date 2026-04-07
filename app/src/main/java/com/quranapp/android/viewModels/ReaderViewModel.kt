package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.QuranPageLineItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.extensions.normalized
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.PageBuilderParams
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

sealed class ReaderViewType {
    data class Chapter(val chapterNo: Int, val verseRange: IntRange? = null) : ReaderViewType()
    data class Juz(val juzNo: Int) : ReaderViewType()
    data class Hizb(val hizbNo: Int) : ReaderViewType()
}

data class ReaderUiState(
    var error: String? = null,
    val viewType: ReaderViewType? = null,
    val currentPageNo: Int? = null,
)

sealed class ReaderIntentData {
    data class FullChapter(val chapterNo: Int) : ReaderIntentData()
    data class FullJuz(val juzNo: Int) : ReaderIntentData()
    data class FullHizb(val hizbNo: Int) : ReaderIntentData()
    data class VerseRange(val chapterNo: Int, val verseRange: IntRange) : ReaderIntentData()

    var slugs: Set<String>? = null
    var readerMode: ReaderMode? = null
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    val controller = RecitationController.getInstance(application)
    val bookmarksRepository = DatabaseProvider.getUserRepository(application)
    val repository = DatabaseProvider.getQuranRepository(application)

    val fontResolver = FontResolver.getInstance(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    var selectedNavigationTabIndex = mutableIntStateOf(0)

    val surahs = repository.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val juzs = repository.getJuzs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hizbs = repository.getHizbs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val readerMode = ReaderPreferences.readerModeFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    var autoScrollSpeed = mutableStateOf<Float?>(null)
    var playerVerseSync = mutableStateOf(false)

    /** Continuously updated by the active mode to track the user's reading position. */
    var lastKnownVerse: ChapterVersePair? = null
        private set

    private val _navigateToPage = MutableStateFlow<Int?>(null)
    val navigateToPage: StateFlow<Int?> = _navigateToPage.asStateFlow()

    private val _navigateToVerse = MutableStateFlow<Pair<Int, Int>?>(null)
    val navigateToVerse: StateFlow<Pair<Int, Int>?> = _navigateToVerse.asStateFlow()

    private val _verseByVerseItems = MutableStateFlow<List<ReaderLayoutItem>>(emptyList())
    val verseByVerseItems: StateFlow<List<ReaderLayoutItem>> =
        _verseByVerseItems.asStateFlow()

    val pageItems = mutableStateMapOf<Int, QuranPageItem>()
    val pageCounts = mutableStateMapOf<Int, Int>()
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
                        slugs = prefs.get(ReaderPreferences.KEY_TRANSLATIONS),
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

        data.readerMode?.let { ReaderPreferences.setReaderMode(it) }
        data.slugs?.let { ReaderPreferences.setTranslations(it) }

        selectedNavigationTabIndex.intValue = when (data) {
            is ReaderIntentData.FullJuz -> 1
            is ReaderIntentData.FullHizb -> 2
            else -> 0
        }

        val state = ReaderUiState().resolveIntent(data)
        _uiState.update { state }
    }

    private fun ReaderUiState.resolveIntent(data: ReaderIntentData): ReaderUiState = when (data) {
        is ReaderIntentData.FullChapter -> {
            if (data.chapterNo in 1..114) copy(viewType = ReaderViewType.Chapter(data.chapterNo))
            else copy(error = context.getString(R.string.strMsgInvalidChapterNo, data.chapterNo))
        }

        is ReaderIntentData.FullJuz -> {
            if (data.juzNo in 1..30) copy(viewType = ReaderViewType.Juz(data.juzNo))
            else copy(error = context.getString(R.string.strMsgInvalidJuzNo, data.juzNo))
        }

        is ReaderIntentData.FullHizb -> {
            if (data.hizbNo in 1..60) copy(viewType = ReaderViewType.Hizb(data.hizbNo))
            else copy(error = context.getString(R.string.strMsgInvalidJuzNo, data.hizbNo))
        }

        is ReaderIntentData.VerseRange -> {
            val chapterNo = data.chapterNo
            if (chapterNo !in 1..114) {
                copy(error = context.getString(R.string.strMsgInvalidChapterNo, chapterNo))
            } else {
                copy(viewType = ReaderViewType.Chapter(chapterNo, data.verseRange.normalized))
            }
        }
    }


    fun updateLastKnownVerseFromItems(firstVisibleIndex: Int) {
        val items = _verseByVerseItems.value
        for (i in firstVisibleIndex until items.size) {
            val item = items[i]
            if (item is ReaderLayoutItem.VerseUI) {
                lastKnownVerse = ChapterVersePair(item.verse)
                return
            }
        }
    }

    fun updateLastKnownVerseFromPage(pageNo: Int) {
        val page = pageItems[pageNo]
        if (page != null) {
            val firstWord = page.lines
                .filterIsInstance<QuranPageLineItem.Text>()
                .firstOrNull()?.words?.firstOrNull()
            if (firstWord != null) {
                lastKnownVerse = QuranUtils.getVerseNo(firstWord.ayahId).let {
                    ChapterVersePair(it.first, it.second)
                }
                return
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val mushafId = ReaderPreferences.getQuranScript()
                .getQuranMushafId(ReaderPreferences.getQuranScriptVariant())
            val ayahId = repository.getFirstAyahIdOnPage(mushafId, pageNo) ?: return@launch
            lastKnownVerse = QuranUtils.getVerseNo(ayahId).let {
                ChapterVersePair(it.first, it.second)
            }
        }
    }

    suspend fun handleModeTransition(to: ReaderMode) {
        val verse = lastKnownVerse ?: return
        val (chapterNo, verseNo) = verse

        when (to) {
            ReaderMode.Reading -> {
                val page = withContext(Dispatchers.IO) {
                    repository.getPageForVerse(chapterNo, verseNo)
                }

                if (page != null) {
                    _uiState.update { it.copy(currentPageNo = page) }
                    selectedNavigationTabIndex.intValue = 3
                    requestPageNavigation(page)
                }
            }

            ReaderMode.VerseByVerse -> {
                val vt = _uiState.value.viewType

                val needsNewChapter = vt !is ReaderViewType.Chapter ||
                        vt.chapterNo != chapterNo ||
                        (vt.verseRange != null && verseNo !in vt.verseRange)

                if (needsNewChapter) {
                    _uiState.update {
                        it.copy(viewType = ReaderViewType.Chapter(chapterNo))
                    }
                    selectedNavigationTabIndex.intValue = 0
                }

                requestVerseNavigation(chapterNo, verseNo)
            }

            else -> {}
        }
    }

    fun requestPageNavigation(pageNo: Int) {
        _navigateToPage.value = pageNo
    }

    fun consumePageNavigation() {
        _navigateToPage.value = null
    }

    fun requestVerseNavigation(chapterNo: Int, verseNo: Int) {
        _navigateToVerse.value = chapterNo to verseNo
    }

    fun consumeVerseNavigation() {
        _navigateToVerse.value = null
    }

    suspend fun syncToPlayingVerse() {
        val verse = controller.state.value.currentVerse
        if (!verse.isValid) return

        val mode = readerMode.value ?: return
        val (chapterNo, verseNo) = verse

        when (mode) {
            ReaderMode.Reading -> {
                val page = withContext(Dispatchers.IO) {
                    repository.getPageForVerse(chapterNo, verseNo)
                }

                if (page != null) requestPageNavigation(page)
            }

            ReaderMode.VerseByVerse -> {
                val isInView = _verseByVerseItems.value.any { item ->
                    item is ReaderLayoutItem.VerseUI &&
                            item.verse.chapterNo == chapterNo &&
                            item.verse.verseNo == verseNo
                }

                if (!isInView) {
                    val state = ReaderUiState(
                        viewType = ReaderViewType.Chapter(chapterNo),
                        currentPageNo = _uiState.value.currentPageNo,
                    )
                    _uiState.update { state }
                }
                requestVerseNavigation(chapterNo, verseNo)
            }

            else -> {}
        }
    }

    private suspend fun buildVerseByVerseItems(
        params: TextBuilderParams,
        state: ReaderUiState,
        readerMode: ReaderMode,
    ) {
        _verseByVerseItems.value = withContext(Dispatchers.IO) {
            when (val vt = state.viewType) {
                is ReaderViewType.Juz -> ReaderItemsBuilder.buildJuzVersesForTranslationMode(
                    context, params, repository, vt.juzNo
                )

                is ReaderViewType.Hizb -> ReaderItemsBuilder.buildHizbVersesForTranslationMode(
                    context, params, repository, vt.hizbNo
                )

                is ReaderViewType.Chapter -> ReaderItemsBuilder.buildVersesForTranslationMode(
                    context, params, vt.chapterNo,
                    vt.verseRange?.first, vt.verseRange?.last,
                )

                null -> emptyList()
            }
        }
    }

    suspend fun mushafPageCount(mushafId: Int): Int {
        return pageCounts.getOrPut(mushafId) {
            repository.getNumberOfPages(mushafId)
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
                repository,
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
    viewType == other.viewType &&
            error == other.error
