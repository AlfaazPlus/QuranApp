package com.quranapp.android.compose.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.TranslationTextStyleParams
import com.quranapp.android.utils.reader.buildTranslationAnnotatedString
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.utils.reader.getTranslationTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SpotlightVersePanel(
    versePair: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs by DataStoreManager.flowMultiple(
        ReaderPreferences.KEY_TRANSLATIONS,
        ReaderPreferences.KEY_SCRIPT,
        ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
    ).collectAsStateWithLifecycle(null)

    if (prefs == null) {
        return
    }

    val preferences = prefs!!
    val slugs = preferences.get(ReaderPreferences.KEY_TRANSLATIONS)
    val scriptCode = QuranScriptUtils.validatePreferredScript(
        preferences.get(ReaderPreferences.KEY_SCRIPT)
    )
    val arabicEnabled = preferences.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED)
    val arabicMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC)
    val translationMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL)

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    val factory = QuranTranslationFactory.remember(context)
    val fontResolver = FontResolver.remember()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (fontResolver == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent<ChapterVersePair>(
                    modifier = Modifier.fillMaxSize(),
                    targetState = versePair,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 1000,
                                delayMillis = 500,
                                easing = FastOutSlowInEasing,
                            ),
                        ) togetherWith fadeOut(
                            animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        )
                    },
                    contentAlignment = Alignment.Center,
                    label = "spotlightVerse",
                ) { pair ->
                    if (!pair.isValid) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else {
                        val chapterNo = pair.chapterNo
                        val verseNo = pair.verseNo

                        val verse by produceState<VerseWithDetails?>(
                            initialValue = null,
                            repository,
                            factory,
                            chapterNo,
                            verseNo,
                            scriptCode,
                            slugs,
                        ) {
                            value = withContext(Dispatchers.IO) {
                                val vwd =
                                    repository.getVerseWithDetails(chapterNo, verseNo, scriptCode)
                                        ?: return@withContext null
                                val aSlug = slugs.firstOrNull() ?: TranslUtils.TRANSL_SLUG_DEFAULT
                                vwd.apply {
                                    translations = factory.getTranslationsSingleVerse(
                                        setOf(aSlug),
                                        chapterNo,
                                        verseNo,
                                    )
                                }
                            }
                        }

                        val scroll = rememberScrollState()
                        LaunchedEffect(pair) {
                            scroll.scrollTo(0)
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (val v = verse) {
                                null -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = colorScheme.primary,
                                        strokeWidth = 3.dp,
                                    )
                                }

                                else -> Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scroll),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    SpotlightQuranText(
                                        vwd = v,
                                        arabicEnabled = arabicEnabled,
                                        fontResolver = fontResolver,
                                        scriptCode = scriptCode,
                                        arabicMultiplier = arabicMultiplier,
                                    )
                                    SpotlightTranslationText(
                                        vwd = v,
                                        factory = factory,
                                        translationMultiplier = translationMultiplier,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightQuranText(
    vwd: VerseWithDetails,
    arabicEnabled: Boolean,
    fontResolver: FontResolver,
    scriptCode: String,
    arabicMultiplier: Float,
) {
    if (!arabicEnabled) return

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val style = remember(
        vwd.pageNo,
        scriptCode,
        arabicMultiplier,
        fontResolver,
        colors,
        type,
    ) {
        getQuranTextStyle(
            QuranTextStyleParams(
                context = context,
                fontResolver = fontResolver,
                colors = colors,
                type = type,
                pageNo = vwd.pageNo,
                script = scriptCode,
                sizeMultiplier = arabicMultiplier,
                useSmallSize = false,
            ),
        ).copy(color = PlayerContentColor)
    }

    val text = remember(vwd.words) {
        buildAnnotatedString {
            vwd.words.forEachIndexed { index, word ->
                append(word.text)
                if (index != vwd.words.lastIndex) {
                    append(" ")
                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = text,
            style = style,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpotlightTranslationText(
    vwd: VerseWithDetails,
    factory: QuranTranslationFactory,
    translationMultiplier: Float,
) {
    val translations = vwd.translations
    if (translations.isEmpty()) return

    SelectionContainer {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            translations.forEach { translation ->
                val tStyle = remember(translation.bookSlug, translationMultiplier) {
                    getTranslationTextStyle(
                        TranslationTextStyleParams(translation.bookSlug, translationMultiplier),
                    ).copy(color = PlayerContentColor.alpha(0.85f))
                }

                Text(
                    text = buildTranslationAnnotatedString(
                        translation,
                        colorScheme,
                        actions = null,
                    ),
                    style = tStyle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
