package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.reader.QuranPageItem
import com.quranapp.android.compose.components.reader.QuranPageLineItem
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.ReaderPreparedData
import com.quranapp.android.compose.components.reader.ReadingFlowPageItem
import com.quranapp.android.compose.components.reader.ReadingFlowSection
import com.quranapp.android.compose.components.reader.TranslationPageItem
import com.quranapp.android.compose.components.reader.TranslationPageSection
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.others.ShortcutUtils
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.QuranScript
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.ReaderChangeManager
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.ReaderObserveAction
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.TranslationPageBuilderParams
import com.quranapp.android.utils.reader.VerseActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import kotlin.time.Duration.Companion.milliseconds

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
    companion object {
        private val readHistoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

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
    var lastKnownVerse: ChapterVersePair? by mutableStateOf(null)
        private set

    var transientHighlightedVerse: ChapterVersePair? by mutableStateOf(null)
        private set
    private var highlightVerseAfterNavigation = false
    private var clearVerseHighlightJob: Job? = null

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
            layout = QuranScript(QuranScriptUtils.SCRIPT_DEFAULT, null),
            pageCount = 0,
            currentPageNo = null,
            version = 0,
        ),
    )
    val mushafSession = _mushafSession.asStateFlow()
    private val mushafSessionMutex = Mutex()

    private var _pageItems = MutableStateFlow<Map<Int, QuranPageItem>>(emptyMap())
    val pageItems = _pageItems.asStateFlow()

    private var _translationPageItems = MutableStateFlow<Map<Int, TranslationPageItem>>(emptyMap())
    val translationPageItems = _translationPageItems.asStateFlow()

    private var _readingFlowPageItems = MutableStateFlow<Map<Int, ReadingFlowPageItem>>(emptyMap())
    val readingFlowPageItems = _readingFlowPageItems.asStateFlow()

    private var lastTranslationReaderContentKey: String? = null
    private var lastPersistedReadHistory: ReadHistoryEntity? = null
    private val readHistorySessionMutex = Mutex()

    private val pagesLoadingMutex = Mutex()
    private val initReaderMutex = Mutex()
    private var lastInitReaderSignature: String? = null
    private var pendingInitMode: ReaderMode? = null

    private val context get() = application

    init {
        controller.connect()

        viewModelScope.launch {
            val code = ReaderPreferences.getQuranScript()
            val variant = ReaderPreferences.getQuranScriptVariant()

            _mushafSession.update {
                it.copy(
                    layout = QuranScript(code, variant),
                )
            }
        }
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
        uiConfig: ComposeUiConfig,
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
                            uiConfig = uiConfig,
                            verseActions = verseActions,
                            fontResolver = fontResolver,
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

    suspend fun initReaderIfNeeded(params: ReaderLaunchParams) {
        val signature = params.toInitSignature()
        var shouldInit = false

        initReaderMutex.withLock {
            if (lastInitReaderSignature != signature) {
                lastInitReaderSignature = signature
                shouldInit = true
            }
        }

        if (!shouldInit && !isReaderAlreadyAt(params.data)) {
            shouldInit = true
        }

        if (!shouldInit) {
            val initialVerse = params.data.initialVerse
            if (initialVerse != null) {
                requestVerseNavigation(initialVerse.chapterNo, initialVerse.verseNo)
            }
            return
        }


        try {
            initReader(params)
        } catch (t: Throwable) {
            initReaderMutex.withLock {
                if (lastInitReaderSignature == signature) {
                    lastInitReaderSignature = null
                }
            }
            throw t
        }
    }

    private fun isReaderAlreadyAt(data: ReaderIntentData): Boolean {
        val currentViewType = _uiState.value.viewType

        return when (data) {
            is ReaderIntentData.FullChapter -> {
                (currentViewType as? ReaderViewType.Chapter)?.chapterNo == data.chapterNo
            }

            is ReaderIntentData.FullJuz -> {
                (currentViewType as? ReaderViewType.Juz)?.juzNo == data.juzNo
            }

            is ReaderIntentData.FullHizb -> {
                (currentViewType as? ReaderViewType.Hizb)?.hizbNo == data.hizbNo
            }

            is ReaderIntentData.MushafPage -> {
                val currentPageNo = _mushafSession.value.currentPageNo
                currentPageNo == data.pageNo
            }
        }
    }

    suspend fun initReader(params: ReaderLaunchParams) {
        Logger.d("INIT Reader with params: $params")
        Logger.d("with data: ${params.data}")

        playerVerseSync.value = true

        // A mode change caused by init must not be treated as a user-driven mode switch, because
        // handleModeTransition() would navigate using a stale lastKnownVerse.
        pendingInitMode = params.readerMode?.takeIf { it != readerMode.value }
        params.readerMode?.let { ReaderPreferences.setReaderMode(it) }
        params.slugs?.let { ReaderPreferences.setTranslations(it) }

        val data = params.data
        val effectiveReaderMode = params.readerMode ?: ReaderPreferences.getReaderMode()

        consumePageNavigation()
        consumeVerseNavigation()

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

        when (effectiveReaderMode) {
            ReaderMode.Reading,
            ReaderMode.Translation -> {
                val script = QuranScript(
                    ReaderPreferences.getQuranScript(),
                    ReaderPreferences.getQuranScriptVariant(),
                )

                if (_mushafSession.value.layout != script) {
                    val pageCount = mushafPageCount(script.toMushafId())

                    _pageItems.value = emptyMap()
                    _translationPageItems.value = emptyMap()
                    _readingFlowPageItems.value = emptyMap()
                    lastTranslationReaderContentKey = null

                    _mushafSession.update {
                        it.copy(
                            layout = script,
                            pageCount = pageCount,
                            version = it.version + 1,
                        )
                    }
                } else {
                    ensureSessionPageCount(script)
                }

                resolveInitialPage(data, state)?.let { targetPage ->
                    setInitialPageTarget(targetPage, data.initialVerse)
                }
            }

            ReaderMode.VerseByVerse -> {
                data.initialVerse?.takeIf { it.isValid }?.let {
                    lastKnownVerse = it
                    requestVerseNavigation(it.chapterNo, it.verseNo)
                }
            }
        }
    }

    private suspend fun resolveInitialPage(
        data: ReaderIntentData,
        state: ReaderUiState,
    ): Int? {
        data.initialVerse?.takeIf { it.isValid }?.let {
            return resolvePageNo(it.chapterNo, it.verseNo)
        }

        return when (val viewType = state.viewType) {
            is ReaderViewType.Chapter -> resolvePageNo(viewType.chapterNo)
            is ReaderViewType.Juz -> withContext(Dispatchers.IO) {
                repository.getFirstPageOfJuz(viewType.juzNo)
            }

            is ReaderViewType.Hizb -> withContext(Dispatchers.IO) {
                repository.getFirstPageOfHizb(viewType.hizbNo)
            }

            null -> null
        }
    }

    private suspend fun setInitialPageTarget(
        pageNo: Int,
        initialVerse: ChapterVersePair?,
    ) {
        _mushafSession.update {
            it.copy(currentPageNo = pageNo)
        }

        val verse = initialVerse?.takeIf { it.isValid }
        lastKnownVerse = verse ?: resolveFirstVerseOnPage(pageNo)

        if (verse != null) {
            requestVerseNavigation(verse.chapterNo, verse.verseNo)
        } else {
            requestPageNavigation(pageNo)
        }
    }

    private suspend fun resolveFirstVerseOnPage(pageNo: Int): ChapterVersePair? {
        val mushafId = _mushafSession.value.layout.toMushafId()
        val ayahId = withContext(Dispatchers.IO) {
            repository.getFirstAyahIdOnPage(mushafId, pageNo)
        } ?: return null

        val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(ayahId)
        return ChapterVersePair(chapterNo, verseNo)
    }

    private suspend fun initMushafPage(
        data: ReaderIntentData.MushafPage,
        readerMode: ReaderMode?,
    ) {
        val targetMode = readerMode ?: ReaderMode.Reading
        pendingInitMode = targetMode.takeIf { it != this.readerMode.value }
        ReaderPreferences.setReaderMode(targetMode)

        // Apply the session before writing the script preference so the observer cannot
        // replace this restored page with a default page during a script switch.
        val script = QuranScript(
            QuranScriptUtils.validatePreferredScript(data.mushafCode),
            data.mushafVariant,
        )
        val mushafId = script.toMushafId()
        val pageCount = mushafPageCount(mushafId)
        val anchorVerse = data.initialVerse?.takeIf { it.isValid }
            ?: withContext(Dispatchers.IO) {
                repository.getFirstAyahIdOnPage(mushafId, data.pageNo)
            }?.let {
                QuranMeta.getVerseNoFromAyahId(it).let { (chapter, verse) ->
                    ChapterVersePair(chapter, verse)
                }
            }

        mushafSessionMutex.withLock {
            val old = _mushafSession.value

            if (old.layout != script) {
                _pageItems.value = emptyMap()
                _translationPageItems.value = emptyMap()
                _readingFlowPageItems.value = emptyMap()
                lastTranslationReaderContentKey = null
            }

            _mushafSession.value = old.copy(
                layout = script,
                pageCount = pageCount,
                currentPageNo = data.pageNo,
                version = if (old.layout != script) old.version + 1 else old.version,
            )

            anchorVerse?.let { lastKnownVerse = it }

            ReaderPreferences.setQuranScriptWithVariant(data.mushafCode, data.mushafVariant)
        }

        _uiState.update {
            ReaderUiState(viewType = ReaderViewType.Chapter(anchorVerse?.chapterNo ?: 1))
        }

        if (anchorVerse != null) {
            requestVerseNavigation(anchorVerse.chapterNo, anchorVerse.verseNo)
        } else {
            requestPageNavigation(data.pageNo)
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


    fun updateLastKnownVerse(verse: ChapterVersePair) {
        if (verse.isValid && lastKnownVerse != verse) lastKnownVerse = verse
    }

    fun saveReadHistory() {
        val state = _uiState.value
        val mushafSession = _mushafSession.value
        val viewType = state.viewType ?: return
        val verse = lastKnownVerse

        readHistoryScope.launch {
            val mode = readerMode.value ?: ReaderPreferences.getReaderMode()
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

            val didSave = readHistorySessionMutex.withLock {
                if (lastPersistedReadHistory == entity) {
                    false
                } else {
                    lastPersistedReadHistory?.let { userRepository.deleteHistory(it.id) }

                    val id = userRepository.saveReadHistory(entity)
                    lastPersistedReadHistory = entity.copy(id = id)
                    true
                }
            }

            if (didSave) {
                ShortcutUtils.pushLastVersesShortcut(context, entity)
            }
        }
    }

    suspend fun handleModeTransition(to: ReaderMode) {
        val initDriven = pendingInitMode
        pendingInitMode = null
        if (initDriven == to) return

        val verse = lastKnownVerse ?: return
        val (chapterNo, verseNo) = verse

        // reset if any
        consumePageNavigation()
        consumeVerseNavigation()

        when (to) {
            ReaderMode.Reading,
            ReaderMode.Translation -> {
                selectedNavigationTabIndex.intValue = 3
                requestVerseNavigation(chapterNo, verseNo)
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

    fun requestVerseNavigation(
        chapterNo: Int,
        verseNo: Int,
        highlightAfterNavigation: Boolean = true,
    ) {
        val verse = ChapterVersePair(chapterNo, verseNo)

        clearTransientVerseHighlight()
        highlightVerseAfterNavigation = highlightAfterNavigation

        _navigateToVerse.value = verse
        lastKnownVerse = verse
    }

    fun consumeVerseNavigation() {
        _navigateToVerse.value = null
        highlightVerseAfterNavigation = false
    }

    fun completeVerseNavigation() {
        val verse = _navigateToVerse.value
        val shouldHighlight = highlightVerseAfterNavigation

        consumeVerseNavigation()

        if (!shouldHighlight || verse == null) return

        clearVerseHighlightJob?.cancel()
        transientHighlightedVerse = verse
        clearVerseHighlightJob = viewModelScope.launch {
            delay(2_850.milliseconds)

            if (transientHighlightedVerse == verse) {
                transientHighlightedVerse = null
                clearVerseHighlightJob = null
            }
        }
    }

    private fun clearTransientVerseHighlight() {
        clearVerseHighlightJob?.cancel()
        clearVerseHighlightJob = null
        transientHighlightedVerse = null
    }

    suspend fun syncToPlayingVerse() {
        val verse = controller.state.value.currentVerse
        if (!verse.isValid) return

        val mode = readerMode.value ?: return
        val (chapterNo, verseNo) = verse

        when (mode) {
            ReaderMode.Reading, ReaderMode.Translation -> {
                requestVerseNavigation(chapterNo, verseNo, highlightAfterNavigation = false)
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

                requestVerseNavigation(chapterNo, verseNo, highlightAfterNavigation = false)
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

                is ReaderViewType.Chapter -> ReaderItemsBuilder.buildChapterVersesForTranslationMode(
                    context, params, vt.chapterNo,
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
        val session = _mushafSession.value
        val builderKey =
            "${session.layout.scriptCode}:${session.layout.variant?.value.orEmpty()}:${params.toKey()}"
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
            fontResolver.prefetch(
                session.layout.scriptCode,
                missing,
                params.isDark,
            )

            ReaderItemsBuilder.buildMushafPages(
                fontResolver,
                missing,
                params,
                session.layout.scriptCode,
                session.layout.variant,
            )
        }

        withContext(Dispatchers.Main.immediate) {
            // A script switch clears the page cache. Ignore an older build that finishes after
            // that clear, or its glyph placements can be reused with the newly selected atlas.
            if (_mushafSession.value.version == session.version) {
                _pageItems.update { old -> old + built }
            }
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

    suspend fun fetchReadingFlowPages(
        context: Context,
        anchorPages: Collection<Int>,
        isDark: Boolean,
    ) {
        val session = _mushafSession.value
        val totalPages = session.pageCount
        val targets = mushafPrefetchTargets(anchorPages, totalPages)
        if (targets.isEmpty()) return

        val cacheKey = "${session.layout.scriptCode}_${session.layout.variant}"

        val missing = pagesLoadingMutex.withLock {
            targets.filter { page ->
                val item = _readingFlowPageItems.value[page]
                item == null || item.cacheKey != cacheKey
            }
        }

        val built = withContext(Dispatchers.IO) {
            fontResolver.prefetch(session.layout.scriptCode, targets.toList(), isDark)

            if (missing.isEmpty()) {
                emptyMap()
            } else {
                ReaderItemsBuilder.buildReadingFlowPages(
                    context,
                    repository,
                    missing,
                    cacheKey,
                )
            }
        }

        if (built.isEmpty()) return

        withContext(Dispatchers.Main.immediate) {
            _readingFlowPageItems.update { old -> old + built }
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

            // Already applied (e.g. by initReader/initMushafPage); don't override its navigation.
            if (old.layout == newScript) {
                if (old.pageCount <= 0) ensureSessionPageCount(newScript)
                return
            }

            val verse = resolveAnchorVerse(old)
            val newCount = mushafPageCount(newScript.toMushafId())

            val newPage = verse?.let {
                repository.getPageForVerse(verse.chapterNo, verse.verseNo, newScript.toMushafId())
            }

            _pageItems.value = emptyMap()
            _translationPageItems.value = emptyMap()
            _readingFlowPageItems.value = emptyMap()

            lastKnownVerse = verse

            _mushafSession.value = old.copy(
                layout = newScript,
                pageCount = newCount,
                currentPageNo = newPage ?: old.currentPageNo?.takeIf { it <= newCount } ?: 1,
                version = old.version + 1,
            )

            // Without an anchor verse keep whatever navigation is already pending.
            if (newPage != null) {
                requestPageNavigation(newPage)
            } else if (_navigateToPage.value == null) {
                requestPageNavigation(1)
            }
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

        val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(ayahId)

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
