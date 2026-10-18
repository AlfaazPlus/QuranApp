package com.quranapp.android.viewModels

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.appLocaleFlow
import com.quranapp.android.compose.utils.formatString
import com.quranapp.android.compose.utils.preferences.DataStoreManager
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.TranslationTextStyleParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.buildTranslationAnnotatedString
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.getTranslationTextStyle
import com.quranapp.android.utils.verse.VerseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class VotdUiState(
    val verseUi: ReaderLayoutItem.VerseUI? = null,
    val arabicEnabled: Boolean = true,
    val arabicTextMultiplier: Float = ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT,
    val scriptCode: String = QuranScriptUtils.SCRIPT_DEFAULT,
    val votdReminderEnabled: Boolean = false,
)

class VotdViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = DatabaseProvider.getQuranRepository(application)
    private val translationFactory = QuranTranslationFactory(application)

    private val _uiState = MutableStateFlow(VotdUiState())
    val uiState: StateFlow<VotdUiState> = _uiState.asStateFlow()

    suspend fun observeVerseUi(
        uiConfig: ComposeUiConfig,
        verseActions: VerseActions,
        atlasBundle: QuranAtlasBundle?,
    ) {
        var contentKey: String? = null

        combine(
            DataStoreManager.flowMultiple(
                ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
                ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
                ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
                ReaderPreferences.KEY_SCRIPT,
                ReaderPreferences.KEY_TRANSLATIONS,
                VersePreferences.KEY_VOTD_REMINDER_ENABLED,
            ),
            appLocaleFlow,
        ) { prefs, locale -> prefs to locale }
            .collectLatest { (prefs, locale) ->
                val arabicEnabled = prefs.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED)
                val arabicTextMultiplier = prefs.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC)
                val translationSizeMultiplier =
                    prefs.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL)
                val scriptCode = prefs.get(ReaderPreferences.KEY_SCRIPT)
                val primarySlug = prefs.get(ReaderPreferences.KEY_TRANSLATIONS)
                    .firstOrNull { !TranslUtils.isTransliteration(it) }
                    ?: TranslUtils.TRANSL_SLUG_DEFAULT

                _uiState.update {
                    it.copy(
                        arabicEnabled = arabicEnabled,
                        arabicTextMultiplier = arabicTextMultiplier,
                        scriptCode = scriptCode,
                        votdReminderEnabled = prefs.get(VersePreferences.KEY_VOTD_REMINDER_ENABLED),
                    )
                }

                val key = "$scriptCode|$primarySlug|$translationSizeMultiplier|" +
                        "${locale.rawLanguageTag}|${locale.numeralSystem?.storageKey}"
                if (key == contentKey && _uiState.value.verseUi != null) return@collectLatest

                val verseUi = withContext(Dispatchers.IO) {
                    buildVerseUi(
                        uiConfig = uiConfig,
                        verseActions = verseActions,
                        primarySlug = primarySlug,
                        translationSizeMultiplier = translationSizeMultiplier,
                        atlasBundle = atlasBundle,
                    )
                } ?: return@collectLatest

                contentKey = key
                _uiState.update { it.copy(verseUi = verseUi) }
            }
    }

    private suspend fun buildVerseUi(
        uiConfig: ComposeUiConfig,
        verseActions: VerseActions,
        primarySlug: String,
        translationSizeMultiplier: Float,
        atlasBundle: QuranAtlasBundle?,
    ): ReaderLayoutItem.VerseUI? {
        val context = uiConfig.context
        val verse = VerseUtils.getVOTD(context, repository) ?: return null
        val slugs = setOf(primarySlug)
        val translations = translationFactory.getTranslationsSingleVerse(
            slugs,
            verse.chapterNo,
            verse.verseNo,
        )
        val booksInfo = translationFactory.getTranslationBooksInfoValidated(slugs)
        val atlasPlacements = atlasBundle?.getPlacementsForWords(verse.words, verse.pageNo)
            ?: emptyMap()
        val serialPattern = context.getString(R.string.strLabelVerseSerialWithChapter)
        val locale = appLocaleFlow.value

        val parsedTranslationTexts = translations.mapNotNull { translation ->
            val bookInfo = booksInfo[translation.bookSlug] ?: return@mapNotNull null
            val textStyle = getTranslationTextStyle(
                TranslationTextStyleParams(
                    translation.bookSlug,
                    uiConfig.type,
                    translationSizeMultiplier,
                )
            )

            val text = buildAnnotatedString {
                withStyle(textStyle.toParagraphStyle()) {
                    withStyle(textStyle.toSpanStyle()) {
                        append(
                            buildTranslationAnnotatedString(
                                translation,
                                uiConfig.colors,
                                actions = VerseActions(
                                    verseActions.onReferenceClick,
                                    onFootnoteClickRaw = { _, footnoteNo ->
                                        verseActions.onFootnoteClick?.invoke(
                                            verse,
                                            translation.footnotes[footnoteNo]
                                        )
                                    }
                                )
                            )
                        )
                    }

                    withStyle(ParagraphStyle(lineHeight = 40.sp)) {
                        withStyle(
                            SpanStyle(
                                color = Color.White.alpha(0.75f),
                                fontStyle = FontStyle.Italic,
                                fontSize = uiConfig.type.labelMedium.fontSize
                            )
                        ) {
                            append(
                                formatString(
                                    context,
                                    locale,
                                    pattern = serialPattern,
                                    verse.chapter.getCurrentName(),
                                    verse.chapterNo,
                                    verse.verseNo
                                )
                            )
                        }
                    }
                }
            }

            bookInfo.langCode to text
        }

        return ReaderLayoutItem.VerseUI(
            verse = verse,
            atlasPlacements = atlasPlacements,
            parsedTranslationTexts = parsedTranslationTexts,
            wbwByWordIndex = null,
            showDivider = false,
            key = "verse-votd"
        )
    }
}
