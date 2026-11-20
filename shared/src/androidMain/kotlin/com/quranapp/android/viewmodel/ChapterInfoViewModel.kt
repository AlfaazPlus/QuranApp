package com.quranapp.android.viewmodel

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.tafsir.ChapterInfoApiResponse
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.quran.RevelationType
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelSurah
import com.quranapp.android.generated.resources.strTitleChapInfoChapterNo
import com.quranapp.android.generated.resources.strTitleChapInfoJuzNo
import com.quranapp.android.generated.resources.strTitleChapInfoRevOrder
import com.quranapp.android.generated.resources.strTitleChapInfoRevType
import com.quranapp.android.generated.resources.strTitleChapInfoRukus
import com.quranapp.android.generated.resources.strTitleMadani
import com.quranapp.android.generated.resources.strTitleMakki
import com.quranapp.android.generated.resources.verses
import com.quranapp.android.platform.Platform
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.colorSchemeFromPreferences
import com.quranapp.android.theme.toCssHex
import com.quranapp.android.theme.toCssRgba
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.chapterInfo.ChapterInfoUtils
import com.quranapp.android.utils.miscellenous.isRtlLanguageCode
import com.quranapp.android.utils.quran.QuranGlyphs
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

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
    data class Init(val context: Context, val chapterNo: Int, val language: String?) :
        ChapterInfoEvent

    data class Retry(val context: Context) : ChapterInfoEvent
}

class ChapterInfoViewModel() : AppViewModel() {

    private val _uiState = MutableStateFlow(ChapterInfoUiState())
    val uiState: StateFlow<ChapterInfoUiState> = _uiState.asStateFlow()

    private val repository = DatabaseProvider.getQuranRepository()

    private var contentLoadJob: Job? = null
    private val lastLangCode = mutableStateOf<String?>(null)

    fun onEvent(event: ChapterInfoEvent) {
        when (event) {
            is ChapterInfoEvent.Init -> {
                viewModelScope.launch {
                    initialize(event.context, event.chapterNo, event.language)
                }
            }

            is ChapterInfoEvent.Retry -> {
                val langCode = lastLangCode.value ?: return
                loadContent(event.context, langCode)
            }
        }
    }

    private suspend fun initialize(context: Context, chapterNo: Int, language: String?) {
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

        loadContent(context, langCode)
    }

    private fun loadContent(context: Context, langCode: String) {
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
                        context,
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
        val chapterInfoFile = ChapterInfoUtils.file(langCode, chapterNo)

        // Try reading from cache first (full JSON) or legacy plain text
        if (chapterInfoFile.toFile().exists()) {
            val cached = AppFiles.readText(chapterInfoFile)

            if (cached.isNotEmpty()) {
                val decoded =
                    runCatching { JsonHelper.json.decodeFromString<ChapterInfoApiResponse>(cached) }
                        .getOrNull()

                if (decoded != null) {
                    return LoadOutcome.Success(decoded)
                }
            }
        } else {
            AppFiles.createFile(chapterInfoFile)
        }

        // Need network
        if (!Platform.networkMonitor.isConnected) {
            return LoadOutcome.NoNetwork
        }

        return try {
            val payload = ApiClient.getChapterInfo(chapterNo, langCode, null)
            val text = payload.chapterInfo.primaryContent()

            if (text.isNotEmpty()) {
                val stored = JsonHelper.json.encodeToString(payload)

                AppFiles.writeText(chapterInfoFile, stored)

                LoadOutcome.Success(payload)
            } else {
                LoadOutcome.Failed("Empty response")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            chapterInfoFile.toFile().delete()
            LoadOutcome.Failed(e.message ?: "Failed to load chapter info")
        }
    }

    private suspend fun buildHtml(context: Context, contentText: String, langCode: String): String {
        val state = _uiState.value
        val meta = state.swl ?: return ""
        val surah = meta.surah
        val isMeccan = surah.revelationType == RevelationType.meccan

        val theme = if (ThemeUtils.isDarkTheme()) "dark" else "light"
        val colorScheme = ThemeUtils.colorSchemeFromPreferences(context)
        val textDirection = if (langCode.isRtlLanguageCode()) "rtl" else "ltr"
        val chapterIcon = QuranGlyphs.Chapter.get(state.chapterNo) + QuranGlyphs.Chapter.getPrefix()

        val juzStr = when {
            state.juzNos.isEmpty() -> ""
            state.juzNos.size == 1 -> state.juzNos.first().toString()
            else -> "${state.juzNos.first()}-${state.juzNos.last()}"
        }

        val chapterTitle = getString(
            Res.string.strLabelSurah,
            meta.getCurrentName(),
        )
        val chapterNoLabel =
            "${getString(Res.string.strTitleChapInfoChapterNo)}: ${state.chapterNo}"

        val revTypeLabel = getString(
            if (isMeccan) Res.string.strTitleMakki
            else Res.string.strTitleMadani,
        )

        val template = Platform.assets.readAssetText("chapter_info/chapter_info_page.html")

        return template
            .replace("{{STYLE}}", buildChapterInfoThemeStyle(colorScheme))
            .replace("{{THEME}}", theme)
            .replace("{{TEXT_DIRECTION}}", textDirection)
            .replace("{{CHAPTER_ICON}}", chapterIcon)
            .replace("{{CHAPTER_TITLE}}", chapterTitle)
            .replace("{{CHAPTER_NO}}", chapterNoLabel)
            .replace(
                "{{JUZ_TITLE}}",
                getString(Res.string.strTitleChapInfoJuzNo)
            )
            .replace("{{JUZ_VALUE}}", juzStr)
            .replace(
                "{{VERSES_TITLE}}",
                getString(Res.string.verses)
            )
            .replace("{{VERSES_VALUE}}", surah.ayahCount.toString())
            .replace(
                "{{RUKUS_TITLE}}",
                getString(Res.string.strTitleChapInfoRukus)
            )
            .replace("{{RUKUS_VALUE}}", surah.rukusCount.toString())
            .replace(
                "{{REV_ORDER_TITLE}}",
                getString(Res.string.strTitleChapInfoRevOrder)
            )
            .replace("{{REV_ORDER_VALUE}}", surah.revelationOrder.toString())
            .replace(
                "{{REV_TYPE_TITLE}}",
                getString(Res.string.strTitleChapInfoRevType)
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

        val file = ChapterInfoUtils.file(langCode, state.chapterNo)
        file.toFile().delete()
    }

    private sealed interface LoadOutcome {
        data class Success(val res: ChapterInfoApiResponse) : LoadOutcome
        object NoNetwork : LoadOutcome
        data class Failed(val message: String) : LoadOutcome
    }
}
