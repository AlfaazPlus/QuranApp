package com.quranapp.android.compose.components.player

import ThemeUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.mediaplayer.RecitationController
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import verticalFadingEdge


@Composable
fun ExpandedPlayerSpotlightSection(
    modifier: Modifier = Modifier,
    verse: ChapterVersePair,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    chromeVisible: Boolean,
    onChromeVisibilityChanged: (Boolean) -> Unit,
) {
    var hideEpoch by remember { mutableIntStateOf(0) }

    fun bumpChrome() {
        onChromeVisibilityChanged(true)
        hideEpoch++
    }

    LaunchedEffect(hideEpoch) {
        delay(3_000)
        onChromeVisibilityChanged(false)
    }

    Column(
        modifier = modifier
    ) {
        SpotlightVersePanel(
            modifier = Modifier
                .weight(1f)
                .pointerInput(chromeVisible) {
                    detectTapGestures {
                        if (chromeVisible) {
                            onChromeVisibilityChanged(false)
                        } else {
                            bumpChrome()
                        }
                    }
                },
            versePair = verse,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = chromeVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize(),
                enter = fadeIn(tween(220)) + slideInVertically(
                    animationSpec = tween(280),
                    initialOffsetY = { it / 2 },
                ),
                exit = fadeOut(tween(180)) + slideOutVertically(
                    animationSpec = tween(220),
                    targetOffsetY = { it / 2 },
                ),
            ) {
                SpotlightPlaybackBar(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    controller = controller,
                )
            }
        }
    }
}

@Composable
private fun SpotlightVersePanel(
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


    if (fontResolver == null) {
        return Loader(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
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

                val scrollState = rememberScrollState()

                LaunchedEffect(pair) {
                    scrollState.scrollTo(0)
                }

                when (val v = verse) {
                    null -> Loader(true)
                    else -> Box(
                        Modifier
                            .verticalFadingEdge(scrollState, color = PlayerBgColor, length = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(24.dp),
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
    val isDarkTheme = ThemeUtils.observeDarkTheme()

    val style = remember(
        vwd.pageNo,
        scriptCode,
        arabicMultiplier,
        fontResolver,
        colors,
        type,
        isDarkTheme
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
                isDark = isDarkTheme
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


@Composable
private fun SpotlightPlaybackBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                controller.previousVerse()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_skip_back),
                contentDescription = stringResource(R.string.strLabelPreviousVerse),
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = {
                controller.seekLeft()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_replay_5),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = colorScheme.primary.copy(alpha = 0.35f),
                    spotColor = colorScheme.primary.copy(alpha = 0.45f),
                )
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        controller.playPause()
                    },
                ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    strokeWidth = 3.dp,
                    color = colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painterResource(
                        if (isPlaying) R.drawable.ic_pause
                        else R.drawable.ic_play,
                    ),
                    contentDescription = if (isPlaying) stringResource(R.string.strLabelPause)
                    else stringResource(R.string.strLabelPlay),
                    modifier = Modifier.size(32.dp),
                    tint = colorScheme.onPrimary,
                )
            }
        }

        IconButton(
            onClick = { controller.seekRight() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_forward_5),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = {
                controller.nextVerse()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_skip_forward),
                contentDescription = stringResource(R.string.strLabelNextVerse),
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }
    }
}