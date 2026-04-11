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
import com.quranapp.android.db.entities.ReadHistoryEntity
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.others.ShortcutUtils
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.QuranScript
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.toQuranMushafId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    data class Chapter(val chapterNo: Int) : ReaderViewType()
    data class Juz(val juzNo: Int) : ReaderViewType()
    data class Hizb(val hizbNo: Int) : ReaderViewType()
}

data class ReaderUiState(
    var error: String? = null,
    val viewType: ReaderViewType? = null,
    val currentPageNo: Int? = null,
)

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

    private val _navigateToVerse = MutableStateFlow<ChapterVersePair?>(null)
    val navigateToVerse: StateFlow<ChapterVersePair?> = _navigateToVerse.asStateFlow()

    private val _verseByVerseItems = MutableStateFlow<List<ReaderLayoutItem>>(emptyList())
    val verseByVerseItems: StateFlow<List<ReaderLayoutItem>> =
        _verseByVerseItems.asStateFlow()

    val pageItems = mutableStateMapOf<Int, QuranPageItem>()
    val pageCounts = mutableStateMapOf<Int, Int>()
    private val mushafPagesInFlight = mutableSetOf<Int>()

    var mushafLayoutKey = mutableStateOf(
        QuranScript(
            ReaderPreferences.getQuranScript(),
            ReaderPreferences.getQuranScriptVariant(),
        )
    )

    private var pageRestoreOnMushafChangeJob: Job? = null

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

    /**
     * Collects UI-driving prefs and rebuilds reader content. Intended to run only while this
     * screen is visible.
     */
    suspend fun observeChanges(
        context: Context,
        colors: ColorScheme,
        type: Typography,
        verseActions: VerseActions
    ) {
        combine(
            _uiState.distinctUntilChanged { old, new -> old.rebuildEquals(new) },
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
            Triple(uiState, prefs, readerMode)
        }
            .collectLatest { (uiState, prefs, readerMode) ->
                val script = QuranScriptUtils.validatePreferredScript(
                    prefs.get(ReaderPreferences.KEY_SCRIPT)
                )
                val scriptVariant = prefs.get(ReaderPreferences.KEY_SCRIPT_VARIANT)

                when (readerMode) {
                    ReaderMode.VerseByVerse -> {
                        val params = TextBuilderParams(
                            context, fontResolver, verseActions, colors, type,
                            arabicEnabled = prefs.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED),
                            arabicSizeMultiplier = prefs.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC),
                            translationSizeMultiplier = prefs.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL),
                            script = script,
                            slugs = prefs.get(ReaderPreferences.KEY_TRANSLATIONS),
                        )
                        buildVerseByVerseItems(params, uiState, readerMode)
                    }

                    ReaderMode.Reading -> {
                        val newLayoutKey = QuranScript.fromRawValues(script, scriptVariant)
                        val oldLayoutKey = mushafLayoutKey.value

                        if (oldLayoutKey != newLayoutKey) {
                            Log.d("CLEARING ITEMS", mushafLayoutKey)

                            mushafPagesInFlight.clear()
                            pageItems.clear()
                            mushafLayoutKey.value = newLayoutKey

                            pageRestoreOnMushafChangeJob?.cancel()
                            pageRestoreOnMushafChangeJob = viewModelScope.launch {
                                restorePageOnMushafChange(oldLayoutKey)
                            }
                        }
                    }

                    else -> {}
                }
            }
    }

    suspend fun initReader(params: ReaderLaunchParams) {
        playerVerseSync.value = true

        params.readerMode?.let { ReaderPreferences.setReaderMode(it) }
        params.slugs?.let { ReaderPreferences.setTranslations(it) }

        val data = params.data

        selectedNavigationTabIndex.intValue = when (data) {
            is ReaderIntentData.FullJuz -> 1
            is ReaderIntentData.FullHizb -> 2
            is ReaderIntentData.MushafPage -> 3
            else -> 0
        }

        // Check if explicitly requested mushaf mode
        if (data is ReaderIntentData.MushafPage) {
            initMushafPage(data)
            return
        }

        val state = ReaderUiState().resolveIntent(data)
        _uiState.update { state }

        Log.d(readerMode.value)

        // Try to navigate to initial verse (works in both mode)
        if (data.initialVerse != null) {
            requestVerseNavigation(data.initialVerse!!.chapterNo, data.initialVerse!!.verseNo)
        }
        // fallback to manual resolution for reader mode
        else if (ReaderPreferences.getReaderMode() != ReaderMode.VerseByVerse) {
            val targetPage = when (state.viewType) {
                is ReaderViewType.Chapter -> {
                    resolvePageNo(state.viewType.chapterNo)
                }

                is ReaderViewType.Juz -> {
                    withContext(Dispatchers.IO) {
                        repository.getFirstPageOfJuz(state.viewType.juzNo)
                    }
                }

                is ReaderViewType.Hizb -> {
                    withContext(Dispatchers.IO) {
                        repository.getFirstPageOfHizb(state.viewType.hizbNo)
                    }
                }

                else -> null
            }

            targetPage?.let { requestPageNavigation(it) }
        }
    }

    private suspend fun initMushafPage(data: ReaderIntentData.MushafPage) {
        ReaderPreferences.setReaderMode(ReaderMode.Reading)

        if (data.mushafCode != null) {
            ReaderPreferences.setQuranScript(data.mushafCode)
            ReaderPreferences.setQuranScriptVariant(data.mushafVariant)

            // Keep in sync with DataStore so [observeChanges] does not treat this as a layout
            // change and run [restorePageOnMushafChange] (which can override the intended page).
            mushafLayoutKey.value = QuranScript(
                ReaderPreferences.getQuranScript(),
                ReaderPreferences.getQuranScriptVariant(),
            )
        }

        if (data.pageNo > 0) {
            _uiState.update {
                ReaderUiState(
                    viewType = ReaderViewType.Chapter(data.fallbackChapterNo.coerceIn(QuranMeta.chapterRange)),
                    currentPageNo = data.pageNo,
                )
            }
            requestPageNavigation(data.pageNo)
        } else if (QuranMeta.isChapterValid(data.fallbackChapterNo) && data.fallbackVerseNo > 0) {
            val page = resolvePageNo(data.fallbackChapterNo, data.fallbackVerseNo, data.mushafCode)

            _uiState.update {
                ReaderUiState(
                    viewType = ReaderViewType.Chapter(data.fallbackChapterNo),
                    currentPageNo = page,
                )
            }
            if (page != null) requestPageNavigation(page)
        } else {
            _uiState.update {
                ReaderUiState(viewType = ReaderViewType.Chapter(1))
            }
        }
    }

    private fun ReaderUiState.resolveIntent(data: ReaderIntentData): ReaderUiState = when (data) {
        is ReaderIntentData.FullChapter -> {
            if (QuranMeta.isChapterValid(data.chapterNo)) copy(
                viewType = ReaderViewType.Chapter(data.chapterNo)
            )
            else copy(error = context.getString(R.string.strMsgInvalidChapterNo, data.chapterNo))
        }

        is ReaderIntentData.FullJuz -> {
            if (QuranMeta.isJuzValid(data.juzNo)) copy(viewType = ReaderViewType.Juz(data.juzNo))
            else copy(error = context.getString(R.string.strMsgInvalidJuzNo, data.juzNo))
        }

        is ReaderIntentData.FullHizb -> {
            if (QuranMeta.isHizbValid(data.hizbNo)) copy(viewType = ReaderViewType.Hizb(data.hizbNo))
            else copy(error = context.getString(R.string.strMsgInvalidJuzNo, data.hizbNo))
        }

        is ReaderIntentData.MushafPage -> {
            this
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
            val ayahId = repository.getFirstAyahIdOnPage(pageNo) ?: return@launch
            lastKnownVerse = QuranUtils.getVerseNo(ayahId).let {
                ChapterVersePair(it.first, it.second)
            }
        }
    }

    fun saveReadHistory() {
        val state = _uiState.value
        val viewType = state.viewType ?: return
        val mode = readerMode.value ?: return
        val verse = lastKnownVerse

        viewModelScope.launch(Dispatchers.IO) {
            val mushafCode = ReaderPreferences.getQuranScript()
            val mushafVariant = ReaderPreferences.getQuranScriptVariant()?.value

            val entity = when (viewType) {
                is ReaderViewType.Chapter -> ReadHistoryEntity(
                    readType = ReadType.Chapter.value,
                    readerMode = mode.value,
                    chapterNo = viewType.chapterNo,
                    fromVerseNo = verse?.verseNo ?: 1,
                    toVerseNo = verse?.verseNo ?: 1,
                    mushafCode = mushafCode,
                    mushafVariant = mushafVariant,
                    pageNo = state.currentPageNo,
                )

                is ReaderViewType.Juz -> ReadHistoryEntity(
                    readType = ReadType.Juz.value,
                    readerMode = mode.value,
                    divisionNo = viewType.juzNo,
                    chapterNo = verse?.chapterNo ?: 0,
                    fromVerseNo = verse?.verseNo ?: 0,
                    toVerseNo = verse?.verseNo ?: 0,
                    mushafCode = mushafCode,
                    mushafVariant = mushafVariant,
                    pageNo = state.currentPageNo,
                )

                is ReaderViewType.Hizb -> ReadHistoryEntity(
                    readType = ReadType.Hizb.value,
                    readerMode = mode.value,
                    divisionNo = viewType.hizbNo,
                    chapterNo = verse?.chapterNo ?: 0,
                    fromVerseNo = verse?.verseNo ?: 0,
                    toVerseNo = verse?.verseNo ?: 0,
                    mushafCode = mushafCode,
                    mushafVariant = mushafVariant,
                    pageNo = state.currentPageNo,
                )
            }

            bookmarksRepository.saveReadHistory(entity)
            ShortcutUtils.pushLastVersesShortcut(context, entity)
        }
    }

    suspend fun handleModeTransition(to: ReaderMode) {
        val verse = lastKnownVerse ?: return
        val (chapterNo, verseNo) = verse

        // reset if any
        consumePageNavigation()
        consumeVerseNavigation()

        when (to) {
            ReaderMode.Reading -> {
                val page = resolvePageNo(chapterNo, verseNo)

                if (page != null) {
                    _uiState.update { it.copy(currentPageNo = page) }
                    selectedNavigationTabIndex.intValue = 3
                    requestPageNavigation(page)
                }
            }

            ReaderMode.VerseByVerse -> {
                val vt = _uiState.value.viewType

                val needsNewChapter = vt !is ReaderViewType.Chapter ||
                        vt.chapterNo != chapterNo

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
        _navigateToVerse.value = ChapterVersePair(chapterNo, verseNo)
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
                val page = resolvePageNo(chapterNo, verseNo)
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
                    context, params, repository, vt.chapterNo,
                )

                null -> emptyList()
            }
        }
    }

    suspend fun resolvePageNo(chapterNo: Int, verseNo: Int = 1, mushafCode: String? = null) =
        withContext(Dispatchers.IO) {
            repository.getPageForVerse(chapterNo, verseNo, mushafCode)
        }

    suspend fun mushafPageCount(mushafId: Int): Int {
        return pageCounts.getOrPut(mushafId) {
            repository.getNumberOfPages(mushafId)
        }
    }

    suspend fun fetchMushafPages(
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

            withContext(Dispatchers.Main) {
                for (page in missing) {
                    built[page]?.let { pageItems[page] = it }
                }
            }
        } finally {
            mushafPagesInFlight.removeAll(missing.toSet())
        }
    }

    /**
     * Resolves reading position after script/mushaf change: [lastKnownVerse] if valid, else first ayah
     * on the current page using the **previous** mushaf layout, then maps that verse to a page in the new mushaf.
     */
    private suspend fun restorePageOnMushafChange(oldLayoutKey: QuranScript) {
        val verseFromMemory = withContext(Dispatchers.Main) {
            lastKnownVerse?.takeIf { it.isValid }
        }

        val versePair = verseFromMemory ?: run {
            val oldMushafId = oldLayoutKey.scriptCode.toQuranMushafId(oldLayoutKey.variant)
            val currentPage = _uiState.value.currentPageNo

            if (currentPage == null || currentPage <= 0 || oldMushafId <= 0) return

            val ayahId = repository.getFirstAyahIdOnPage(oldMushafId, currentPage) ?: return
            val (c, v) = QuranUtils.getVerseNo(ayahId)

            ChapterVersePair(c, v)
        }

        val newPage = withContext(Dispatchers.IO) {
            repository.getPageForVerse(versePair.chapterNo, versePair.verseNo)
        } ?: return

        withContext(Dispatchers.Main) {
            lastKnownVerse = versePair
            _uiState.update { it.copy(currentPageNo = newPage) }

            requestPageNavigation(newPage)
        }
    }
}

const val MUSHAF_PREFETCH_RADIUS = 4

private fun ReaderUiState.rebuildEquals(other: ReaderUiState): Boolean =
    viewType == other.viewType &&
            error == other.error
