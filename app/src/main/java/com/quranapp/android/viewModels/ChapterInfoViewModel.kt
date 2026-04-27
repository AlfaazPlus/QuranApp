package com.quranapp.android.viewModels

import ThemeUtils
import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.tafsir.ChapterInfoApiResponse
import com.quranapp.android.compose.theme.toCssHex
import com.quranapp.android.compose.theme.toCssRgba
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.quran.RevelationType
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.utils.quran.QuranGlyphs
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.ResUtils
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString


sealed interface ChapterInfoContentState {
    object Loading : ChapterInfoContentState
    data class Success(val html: String, val langCode: String) : ChapterInfoContentState
    data class Error(val message: String, val canRetry: Boolean) : ChapterInfoContentState
    object NoInternet : ChapterInfoContentState
    object InvalidParams : ChapterInfoContentState
}

data class ChapterInfoUiState(
    val chapterNo: Int = 0,
    val swl: SurahWithLocalizations? = null,
    val juzNos: List<Int> = emptyList(),
    val contentState: ChapterInfoContentState = ChapterInfoContentState.Loading,
)

sealed interface ChapterInfoEvent {
    data class Init(val chapterNo: Int, val language: String?) : ChapterInfoEvent
    object Retry : ChapterInfoEvent
}

class ChapterInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChapterInfoUiState())
    val uiState: StateFlow<ChapterInfoUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()
    private val repository = DatabaseProvider.getQuranRepository(context)
    private val fileUtils = FileUtils.newInstance(context)

    private var contentLoadJob: Job? = null
    private val lastLangCode = mutableStateOf<String?>(null)

    fun onEvent(event: ChapterInfoEvent) {
        when (event) {
            is ChapterInfoEvent.Init -> {
                viewModelScope.launch {
                    initialize(event.chapterNo, event.language)
                }
            }

            is ChapterInfoEvent.Retry -> {
                val langCode = lastLangCode.value ?: return
                loadContent(langCode)
            }
        }
    }

    private suspend fun initialize(chapterNo: Int, language: String?) {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            _uiState.update {
                it.copy(
                    chapterNo = chapterNo,
                    contentState = ChapterInfoContentState.InvalidParams
                )
            }
            return
        }

        val langCode = language?.takeIf { it.isNotEmpty() } ?: "en"

        val chapterMeta = withContext(Dispatchers.IO) {
            repository.getSurahWithLocalizations(chapterNo)
        }

        if (chapterMeta == null) {
            _uiState.update {
                it.copy(
                    chapterNo = chapterNo,
                    contentState = ChapterInfoContentState.InvalidParams
                )
            }
            return
        }

        val juzNos = withContext(Dispatchers.IO) {
            repository.getJuzNosForChapter(chapterNo)
        }

        _uiState.update {
            it.copy(
                chapterNo = chapterNo,
                swl = chapterMeta,
                juzNos = juzNos,
                contentState = ChapterInfoContentState.Loading,
            )
        }

        loadContent(langCode)
    }

    private fun loadContent(langCode: String) {
        val state = _uiState.value
        if (state.chapterNo < 1) return

        lastLangCode.value = langCode

        contentLoadJob?.cancel()
        contentLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(contentState = ChapterInfoContentState.Loading) }

            val outcome = try {
                withContext(Dispatchers.IO) {
                    loadChapterInfoData(langCode, state.chapterNo)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                LoadOutcome.Failed(e.message ?: "Failed to load chapter info")
            }

            when (outcome) {
                is LoadOutcome.Success -> {
                    val resultLangCode = outcome.res.chapterInfo.languageCode ?: "en"
                    val html = buildHtml(
                        outcome.res.chapterInfo.primaryContent(),
                        resultLangCode
                    )

                    _uiState.update {
                        it.copy(
                            contentState = ChapterInfoContentState.Success(html, resultLangCode)
                        )
                    }
                }

                LoadOutcome.NoNetwork -> _uiState.update {
                    it.copy(contentState = ChapterInfoContentState.NoInternet)
                }

                is LoadOutcome.Failed -> {
                    deleteSavedFileIfExists(langCode)

                    _uiState.update {
                        it.copy(
                            contentState = ChapterInfoContentState.Error(
                                outcome.message,
                                true
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadChapterInfoData(langCode: String, chapterNo: Int): LoadOutcome {
        val chapterInfoFile = fileUtils.getChapterInfoFile(langCode, chapterNo)

        // Try reading from cache first (full JSON) or legacy plain text
        if (chapterInfoFile.exists()) {
            val cached = fileUtils.readText(chapterInfoFile)

            if (cached.isNotEmpty()) {
                val decoded =
                    runCatching { JsonHelper.json.decodeFromString<ChapterInfoApiResponse>(cached) }
                        .getOrNull()

                if (decoded != null) {
                    return LoadOutcome.Success(decoded)
                }
            }
        } else {
            fileUtils.createFile(chapterInfoFile)
        }

        // Need network
        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            return LoadOutcome.NoNetwork
        }

        return try {
            val payload = RetrofitInstance.alfaazplus.getChapterInfo(chapterNo, langCode, null)
            val text = payload.chapterInfo.primaryContent()

            if (text.isNotEmpty()) {
                val stored = JsonHelper.json.encodeToString(payload)

                fileUtils.writeToFile(chapterInfoFile, stored)

                LoadOutcome.Success(payload)
            } else {
                LoadOutcome.Failed("Empty response")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            chapterInfoFile.delete()
            LoadOutcome.Failed(e.message ?: "Failed to load chapter info")
        }
    }

    private fun buildHtml(contentText: String, langCode: String): String {
        val state = _uiState.value
        val meta = state.swl ?: return ""
        val surah = meta.surah
        val isMeccan = surah.revelationType == RevelationType.meccan

        val theme = if (ThemeUtils.isDarkTheme(context)) "dark" else "light"
        val colorScheme = ThemeUtils.colorSchemeFromPreferences(context)
        val textDirection = if (StringUtils.isRtlLanguage(langCode)) "rtl" else "ltr"
        val chapterIcon = QuranGlyphs.Chapter.get(state.chapterNo) + QuranGlyphs.Chapter.getPrefix()

        val juzStr = when {
            state.juzNos.isEmpty() -> ""
            state.juzNos.size == 1 -> state.juzNos.first().toString()
            else -> "${state.juzNos.first()}-${state.juzNos.last()}"
        }

        val chapterTitle = context.getString(
            R.string.strLabelSurah,
            meta.getCurrentName(),
        )
        val chapterNoLabel =
            "${context.getString(R.string.strTitleChapInfoChapterNo)}: ${state.chapterNo}"

        val revTypeLabel = context.getString(
            if (isMeccan) R.string.strTitleMakki
            else R.string.strTitleMadani,
        )

        val template = ResUtils.readAssetsTextFile(context, "chapter_info/chapter_info_page.html")

        return template
            .replace("{{STYLE}}", buildChapterInfoThemeStyle(colorScheme))
            .replace("{{THEME}}", theme)
            .replace("{{TEXT_DIRECTION}}", textDirection)
            .replace("{{CHAPTER_ICON}}", chapterIcon)
            .replace("{{CHAPTER_TITLE}}", chapterTitle)
            .replace("{{CHAPTER_NO}}", chapterNoLabel)
            .replace(
                "{{JUZ_TITLE}}",
                context.getString(R.string.strTitleChapInfoJuzNo)
            )
            .replace("{{JUZ_VALUE}}", juzStr)
            .replace(
                "{{VERSES_TITLE}}",
                context.getString(R.string.strTitleChapInfoVerses)
            )
            .replace("{{VERSES_VALUE}}", surah.ayahCount.toString())
            .replace(
                "{{RUKUS_TITLE}}",
                context.getString(R.string.strTitleChapInfoRukus)
            )
            .replace("{{RUKUS_VALUE}}", surah.rukusCount.toString())
            .replace(
                "{{REV_ORDER_TITLE}}",
                context.getString(R.string.strTitleChapInfoRevOrder)
            )
            .replace("{{REV_ORDER_VALUE}}", surah.revelationOrder.toString())
            .replace(
                "{{REV_TYPE_TITLE}}",
                context.getString(R.string.strTitleChapInfoRevType)
            )
            .replace("{{REV_TYPE_VALUE}}", revTypeLabel)
            .replace("{{CONTENT}}", contentText)
    }

    private fun buildChapterInfoThemeStyle(scheme: ColorScheme): String {
        val vars = linkedMapOf(
            "--color-primary" to scheme.primary.toCssHex(),
            "--color-on-primary" to scheme.onPrimary.toCssHex(),
            "--color-background" to scheme.background.toCssHex(),
            "--color-surface" to scheme.surface.toCssHex(),
            "--color-on-surface" to scheme.onSurface.toCssHex(),
            "--color-on-surface-variant" to scheme.onSurfaceVariant.toCssHex(),
            "--color-surface-variant" to scheme.surfaceVariant.toCssHex(),
            "--color-outline-variant" to scheme.outlineVariant.toCssHex(),
            "--color-primary-container" to scheme.primaryContainer.toCssHex(),
            "--color-on-primary-container" to scheme.onPrimaryContainer.toCssHex(),
            "--color-surface-container" to scheme.surfaceContainer.toCssHex(),
            "--color-link-bg" to scheme.primary.copy(alpha = 0.2f).toCssRgba(),
            "--color-link-bg-active" to scheme.primary.copy(alpha = 0.3f).toCssRgba(),
        )

        val css = vars.entries.joinToString("") { "${it.key}:${it.value};" }

        return "<style>:root{$css}</style>"
    }

    private fun deleteSavedFileIfExists(langCode: String) {
        val state = _uiState.value

        val file = fileUtils.getChapterInfoFile(langCode, state.chapterNo)
        file.delete()
    }

    private sealed interface LoadOutcome {
        data class Success(val res: ChapterInfoApiResponse) : LoadOutcome
        object NoNetwork : LoadOutcome
        data class Failed(val message: String) : LoadOutcome
    }
}
