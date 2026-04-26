package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.QuranPageLineItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.ReaderPreparedData
import com.quranapp.android.compose.components.reader.TranslationPageItem
import com.quranapp.android.compose.components.reader.TranslationPageSection
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.ReadHistoryEntity
import com.quranapp.android.utils.others.ShortcutUtils
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.QuranScript
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.ReaderChangeManager
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.ReaderObserveAction
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.TranslationPageBuilderParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.toQuranMushafId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class ReaderViewType {
    data class Chapter(val chapterNo: Int) : ReaderViewType()
    data class Juz(val juzNo: Int) : ReaderViewType()
    data class Hizb(val hizbNo: Int) : ReaderViewType()
}

data class ReaderUiState(
    var error: String? = null,
    val viewType: ReaderViewType? = null,
)

data class MushafSession(
    val layout: QuranScript,
    val pageCount: Int,
    val currentPageNo: Int?,
    val version: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(application: Application) : ReaderProviderViewModel(application) {
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

    private val _verseByVersePrepared = MutableStateFlow(
        ReaderPreparedData(emptyList(), emptyMap()),
    )

    val verseByVersePrepared: StateFlow<ReaderPreparedData> =
        _verseByVersePrepared.asStateFlow()

    private val _mushafSession = MutableStateFlow(
        MushafSession(
            layout = QuranScript(
                ReaderPreferences.getQuranScript(),
                ReaderPreferences.getQuranScriptVariant(),
            ),
            pageCount = 0,
            currentPageNo = null,
            version = 0,
        )
    )
    val mushafSession = _mushafSession.asStateFlow()
    private val mushafSessionMutex = Mutex()

    private var _pageItems = MutableStateFlow<Map<Int, QuranPageItem>>(emptyMap())
    val pageItems = _pageItems.asStateFlow()

    private var _translationPageItems = MutableStateFlow<Map<Int, TranslationPageItem>>(emptyMap())
    val translationPageItems = _translationPageItems.asStateFlow()

    private var lastTranslationReaderContentKey: String? = null

    private val pagesLoadingMutex = Mutex()

    private val context get() = application

    init {
        controller.connect()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }

    fun updateCurrentPageNo(pageNo: Int) {
        _mushafSession.update { it.copy(currentPageNo = pageNo) }
    }

    /**
     * Collects UI-driving prefs and rebuilds reader content. Intended to run only while this
     * screen is visible.
     */
    suspend fun observeChanges(
        context: Context,
        colors: ColorScheme,
        type: Typography,
        verseActions: VerseActions,
    ) {
        readerMode
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { mode ->
                when (mode) {
                    ReaderMode.VerseByVerse -> combine(
                        ReaderChangeManager.verseModeFlow(),
                        _uiState.map { it.viewType }.distinctUntilChanged(),
                    ) { action, _ ->
                        action
                    }
                    ReaderMode.Reading -> ReaderChangeManager.mushafModeFlow()
                    ReaderMode.Translation -> ReaderChangeManager.translationModeFlow()
                }
            }
            .collectLatest { action ->
                when (action) {
                    is ReaderObserveAction.BuildVerse -> {
                        val params = TextBuilderParams(
                            context = context,
                            fontResolver = fontResolver,
                            verseActions = verseActions,
                            colors = colors,
                            type = type,
                            arabicEnabled = action.cfg.arabicEnabled,
                            arabicSizeMultiplier = action.cfg.arabicSize,
                            translationSizeMultiplier = action.cfg.translationSize,
                            script = action.cfg.script.scriptCode,
                            slugs = action.cfg.translations,
                        )

                        buildVerseByVerseItems(
                            params = params,
                            state = _uiState.value,
                            readerMode = ReaderMode.VerseByVerse,
                        )
                    }

                    is ReaderObserveAction.SwitchMushaf -> {
                        val layout = action.cfg.script

                        if (_mushafSession.value.layout != layout) {
                            switchScript(layout)
                        } else if (_mushafSession.value.pageCount <= 0) {
                            ensureSessionPageCount(layout)
                        }
                    }

                    is ReaderObserveAction.BuildTranslation -> {
                        val layout = action.cfg.script

                        if (_mushafSession.value.layout != layout) {
                            lastTranslationReaderContentKey = null
                            switchScript(layout)
                        } else if (_mushafSession.value.pageCount <= 0) {
                            ensureSessionPageCount(layout)
                        }

                        val key = action.cfg.toCacheKey()

                        if (lastTranslationReaderContentKey != key) {
                            _translationPageItems.value = emptyMap()
                            lastTranslationReaderContentKey = key
                        }
                    }
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
            initMushafPage(data, params.readerMode)
            return
        }

        val state = ReaderUiState().resolveIntent(data)
        _uiState.update { state }

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

    private suspend fun initMushafPage(
        data: ReaderIntentData.MushafPage,
        readerMode: ReaderMode?,
    ) {
        ReaderPreferences.setReaderMode(readerMode ?: ReaderMode.Reading)

        if (data.mushafCode != null) {
            ReaderPreferences.setQuranScriptWithVariant(data.mushafCode, data.mushafVariant)

            val script = QuranScript(
                ReaderPreferences.getQuranScript(),
                ReaderPreferences.getQuranScriptVariant(),
            )
            val pageCount = mushafPageCount(script.toMushafId())

            _mushafSession.update {
                it.copy(
                    layout = script,
                    pageCount = pageCount,
                )
            }
        }

        if (data.pageNo > 0) {
            _uiState.update {
                ReaderUiState(
                    viewType = ReaderViewType.Chapter(data.fallbackChapterNo.coerceIn(QuranMeta.chapterRange)),
                )
            }

            requestPageNavigation(data.pageNo)
            // explicit mushaf page wins over [initialVerse] for positioning; verse is only for
            // history/sync — do not call [requestVerseNavigation] or translation/mushaf UI will
            // resolve the verse to a (possibly different) page after scroll.
            data.initialVerse?.takeIf { it.isValid }?.let { lastKnownVerse = it }
        } else if (QuranMeta.isChapterValid(data.fallbackChapterNo) && data.fallbackVerseNo > 0) {
            val page = resolvePageNo(
                data.fallbackChapterNo,
                data.fallbackVerseNo,
                data.mushafCode?.toQuranMushafId(data.mushafVariant)
            )

            _uiState.update {
                ReaderUiState(
                    viewType = ReaderViewType.Chapter(data.fallbackChapterNo),
                )
            }

            if (page != null) requestPageNavigation(page)

            lastKnownVerse = data.initialVerse?.takeIf { it.isValid }
                ?: ChapterVersePair(data.fallbackChapterNo, data.fallbackVerseNo)
        } else {
            _uiState.update {
                ReaderUiState(viewType = ReaderViewType.Chapter(1))
            }
            data.initialVerse?.takeIf { it.isValid }?.let {
                requestVerseNavigation(it.chapterNo, it.verseNo)
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
        val items = _verseByVersePrepared.value.items

        for (i in firstVisibleIndex until items.size) {
            val item = items[i]
            if (item is ReaderLayoutItem.VerseUI) {
                lastKnownVerse = ChapterVersePair(item.verse)
                return
            }
        }
    }

    fun updateLastKnownVerseFromPage(pageNo: Int) {
        val page = _pageItems.value[pageNo]

        if (page != null) {
            val firstWord = page.lines
                .filterIsInstance<QuranPageLineItem.Text>()
                .firstOrNull()?.words?.firstOrNull()
            if (firstWord != null) {
                lastKnownVerse = QuranUtils.getVerseNoFromAyahId(firstWord.ayahId).let {
                    ChapterVersePair(it.first, it.second)
                }
                return
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val ayahId = repository.getFirstAyahIdOnPage(pageNo) ?: return@launch
            lastKnownVerse = QuranUtils.getVerseNoFromAyahId(ayahId).let {
                ChapterVersePair(it.first, it.second)
            }
        }
    }

    fun updateLastKnownVerseFromTranslationPage(pageNo: Int) {
        val page = _translationPageItems.value[pageNo]

        if (page != null) {
            val firstVerse = page.sections
                .filterIsInstance<TranslationPageSection.Text>()
                .firstOrNull()
                ?.verses
                ?.firstOrNull()

            if (firstVerse != null) {
                lastKnownVerse = ChapterVersePair(firstVerse.chapterNo, firstVerse.verseNo)
                return
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val ayahId = repository.getFirstAyahIdOnPage(pageNo) ?: return@launch

            lastKnownVerse = QuranUtils.getVerseNoFromAyahId(ayahId).let {
                ChapterVersePair(it.first, it.second)
            }
        }
    }

    fun saveReadHistory() {
        val state = _uiState.value
        val mushafSession = _mushafSession.value
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
                    pageNo = mushafSession.currentPageNo,
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
                    pageNo = mushafSession.currentPageNo,
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
                    pageNo = mushafSession.currentPageNo,
                )
            }

            userRepository.saveReadHistory(entity)
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
            ReaderMode.Reading,
            ReaderMode.Translation -> {
                val page = _mushafSession.value.currentPageNo ?: resolvePageNo(chapterNo, verseNo)

                if (page != null) {
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
            ReaderMode.Reading, ReaderMode.Translation -> {
                val page = resolvePageNo(chapterNo, verseNo)
                if (page != null) requestPageNavigation(page)
            }

            ReaderMode.VerseByVerse -> {
                val isInView = _verseByVersePrepared.value.items.any { item ->
                    item is ReaderLayoutItem.VerseUI &&
                            item.verse.chapterNo == chapterNo &&
                            item.verse.verseNo == verseNo
                }

                if (!isInView) {
                    val state = ReaderUiState(
                        viewType = ReaderViewType.Chapter(chapterNo),
                    )

                    _uiState.update { state }
                }

                requestVerseNavigation(chapterNo, verseNo)
            }
        }
    }

    private suspend fun buildVerseByVerseItems(
        params: TextBuilderParams,
        state: ReaderUiState,
        readerMode: ReaderMode,
    ) {
        _verseByVersePrepared.value = withContext(Dispatchers.IO) {
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

                null -> ReaderPreparedData(emptyList(), emptyMap())
            }
        } ?: ReaderPreparedData(emptyList(), emptyMap())
    }

    suspend fun resolvePageNo(chapterNo: Int, verseNo: Int = 1, mushafId: Int? = null): Int? {
        val mId = mushafId ?: _mushafSession.value.layout.toMushafId()

        return withContext(Dispatchers.IO) {
            repository.getPageForVerse(chapterNo, verseNo, mId)
        }
    }

    suspend fun mushafPageCount(mushafId: Int): Int {
        return repository.getNumberOfPages(mushafId)
    }

    suspend fun fetchMushafPages(
        context: Context,
        anchorPages: Collection<Int>,
        params: PageBuilderParams
    ) {
        val builderKey = params.toKey()
        val session = _mushafSession.value
        val totalPages = session.pageCount
        val targets = mushafPrefetchTargets(anchorPages, totalPages)
        if (targets.isEmpty()) return

        val missing = pagesLoadingMutex.withLock {
            targets.filter { page ->
                val item = _pageItems.value[page]

                item == null || item.cacheKey != builderKey
            }
        }

        if (missing.isEmpty()) return

        val built = withContext(Dispatchers.IO) {
            fontResolver.prefetch(session.layout.scriptCode, missing)

            ReaderItemsBuilder.buildMushafPages(
                repository,
                fontResolver,
                missing,
                params
            )
        }

        withContext(Dispatchers.Main.immediate) {
            _pageItems.update { old -> old + built }
        }
    }

    suspend fun fetchTranslationPages(
        context: Context,
        anchorPages: Collection<Int>,
        buildParams: TranslationPageBuilderParams,
    ) {
        val session = _mushafSession.value
        val totalPages = session.pageCount

        val targets = mushafPrefetchTargets(anchorPages, totalPages)
        if (targets.isEmpty()) return

        val missing = pagesLoadingMutex.withLock {
            targets.filter { page ->
                !_translationPageItems.value.containsKey(page)
            }
        }

        if (missing.isEmpty()) return

        val slug = ReaderPreferences.primaryTranslationSlug()

        val built = withContext(Dispatchers.IO) {
            ReaderItemsBuilder.buildTranslationPages(
                context,
                repository,
                missing,
                slug,
                buildParams,
            )
        }

        withContext(Dispatchers.Main.immediate) {
            _translationPageItems.update { old -> old + built }
        }
    }

    private suspend fun ensureSessionPageCount(script: QuranScript) {
        val current = _mushafSession.value

        if (current.pageCount > 0 || current.layout != script) return

        val pageCount = mushafPageCount(script.toMushafId())

        _mushafSession.update {
            it.copy(
                pageCount = pageCount,
                version = it.version + 1
            )
        }
    }

    suspend fun switchScript(newScript: QuranScript) {
        mushafSessionMutex.withLock {
            val old = _mushafSession.value

            val verse = resolveAnchorVerse(old)
            val newCount = mushafPageCount(newScript.toMushafId())

            val newPage = verse?.let {
                repository.getPageForVerse(verse.chapterNo, verse.verseNo, newScript.toMushafId())
            } ?: 1

            _pageItems.value = emptyMap()
            _translationPageItems.value = emptyMap()

            lastKnownVerse = verse

            _mushafSession.value = old.copy(
                layout = newScript,
                pageCount = newCount,
                currentPageNo = newPage,
                version = old.version + 1,
            )

            requestPageNavigation(newPage)
        }
    }

    private suspend fun resolveAnchorVerse(session: MushafSession): ChapterVersePair? {
        val verseFromMemory = withContext(Dispatchers.Main.immediate) {
            lastKnownVerse?.takeIf { it.isValid }
        }

        if (verseFromMemory != null) return verseFromMemory

        val currentPage = session.currentPageNo ?: return null
        val oldMushafId = session.layout.toMushafId()

        if (currentPage <= 0 || oldMushafId <= 0) return null

        val ayahId = repository.getFirstAyahIdOnPage(oldMushafId, currentPage) ?: return null

        val (chapterNo, verseNo) = QuranUtils.getVerseNoFromAyahId(ayahId)

        return ChapterVersePair(chapterNo, verseNo)
    }
}

const val MUSHAF_PREFETCH_RADIUS = 4

private fun mushafPrefetchTargets(anchorPages: Collection<Int>, totalPages: Int): Set<Int> {
    if (totalPages <= 0) return emptySet()

    val targets = linkedSetOf<Int>()

    for (anchorPage in anchorPages) {
        if (anchorPage !in 1..totalPages) continue
        for (d in -MUSHAF_PREFETCH_RADIUS..MUSHAF_PREFETCH_RADIUS) {
            val page = anchorPage + d
            if (page in 1..totalPages) targets += page
        }
    }

    return targets
}

private fun ReaderUiState.rebuildEquals(other: ReaderUiState): Boolean =
    viewType == other.viewType &&
            error == other.error
