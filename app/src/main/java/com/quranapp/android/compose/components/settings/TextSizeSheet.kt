package com.quranapp.android.compose.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.components.reader.QuranWordText
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MAX_PROGRESS
import com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MIN_PROGRESS
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.LocalQuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.getForWord
import com.quranapp.android.utils.reader.atlas.rememberQuranAtlasBundle
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.utils.reader.isQuranAtlasScript
import com.quranapp.android.utils.reader.toQuranMushafId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREVIEW_SURAH = 1
private const val PREVIEW_AYAH = 1

private data class ArabicPreviewLoaded(
    val words: List<AyahWordEntity>,
    val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
    val pageNo: Int,
)

@Composable
fun TextSizeSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.icon_font_size,
        title = stringResource(R.string.textSizes),
    ) {
        AlertCard(
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                stringResource(R.string.textSizeVerseByVerseOnly),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ArabicTextSizeSlider()
            TranslationTextSizeSlider()
        }
    }
}


@Composable
private fun ArabicTextSizeSlider() {
    val coroutineScope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val textSizeMult = ReaderPreferences.observeArabicTextSizeMultiplier()

    val min = TEXT_SIZE_MIN_PROGRESS.toFloat()
    val max = TEXT_SIZE_MAX_PROGRESS.toFloat()
    val steps = max.toInt() - min.toInt()
    val progress = textSizeMult * 100

    ListItemCategoryLabel(stringResource(R.string.strTitleReaderTextSizeArabic))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            value = progress,
            onValueChange = {
                coroutineScope.launch {
                    ReaderPreferences.setArabicTextSizeMultiplier(
                        ReaderTextSizeUtils.calculateMultiplier(it.toInt())
                    )
                }
            },
            valueRange = min..max,
            steps = steps
        )
        Text(
            text = String.format(appLocale.platformLocale, "%d%%", progress.toInt()),
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        ArabicTextPreview(textSizeMult)
    }
}

@Composable
private fun ArabicTextPreview(textSizeMult: Float) {
    val context = LocalContext.current
    val script = ReaderPreferences.observeQuranScript()
    val isDark = ThemeUtils.observeDarkTheme()
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val fontResolver = remember(context) { FontResolver.getInstance(context.applicationContext) }
    val externalDb = remember(context) { DatabaseProvider.getExternalQuranDatabase(context) }
    val atlasBundle = rememberQuranAtlasBundle(externalDb)

    val loaded by produceState<ArabicPreviewLoaded?>(
        initialValue = null,
        script,
        atlasBundle,
    ) {
        value = withContext(Dispatchers.IO) {
            val repo = DatabaseProvider.getQuranRepository(context)
            val variant = ReaderPreferences.getQuranScriptVariant()
            val mushafId = script.toQuranMushafId(variant)

            val words = repo.getWordsForAyah(PREVIEW_SURAH, PREVIEW_AYAH, script)

            if (words.isEmpty()) return@withContext null

            val pageNo = repo.getPageForVerse(PREVIEW_SURAH, PREVIEW_AYAH, mushafId) ?: 1

            val placements = if (script.isQuranAtlasScript()) {
                val b =
                    atlasBundle ?: return@withContext ArabicPreviewLoaded(words, emptyMap(), pageNo)
                b.getPlacementsForWords(words)
            } else {
                emptyMap()
            }

            ArabicPreviewLoaded(words, placements, pageNo)
        }
    }

    val pageNo = loaded?.pageNo ?: 1

    val style = remember(script, textSizeMult, isDark, colors, typography, pageNo) {
        getQuranTextStyle(
            QuranTextStyleParams(
                context = context,
                fontResolver = fontResolver,
                colors = colors,
                type = typography,
                pageNo = pageNo,
                script = script,
                sizeMultiplier = textSizeMult,
                isDark = isDark,
            ),
        )
    }

    val previewData = loaded

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        CompositionLocalProvider(LocalQuranAtlasBundle provides atlasBundle) {
            when {
                previewData == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                previewData.words.isEmpty() -> {
                    Text(
                        text = "…",
                        style = style,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                else -> {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (word in previewData.words) {
                            val placements = if (script.isQuranAtlasScript()) {
                                previewData.atlasPlacements.getForWord(word)
                                    ?.takeIf { it.isNotEmpty() }
                            } else {
                                null
                            }
                            QuranWordText(
                                word = word,
                                atlasPlacements = placements,
                                style = style,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationTextSizeSlider() {
    val coroutineScope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val textSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()

    val min = TEXT_SIZE_MIN_PROGRESS.toFloat()
    val max = TEXT_SIZE_MAX_PROGRESS.toFloat()
    val steps = max.toInt() - min.toInt()
    val progress = textSizeMult * 100

    ListItemCategoryLabel(stringResource(R.string.strTitleReaderTextSizeTransl))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            value = progress,
            onValueChange = {
                coroutineScope.launch {
                    ReaderPreferences.setTranslationTextSizeMultiplier(
                        ReaderTextSizeUtils.calculateMultiplier(
                            it.toInt()
                        )
                    )
                }
            },
            valueRange = min..max,
            steps = steps
        )
        Text(
            text = String.format(appLocale.platformLocale, "%d%%", progress.toInt()),
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        TranslationTextPreview(textSizeMult)
    }
}

@Composable
private fun TranslationTextPreview(textSizeMult: Float) {
    val fontSize = 16.sp * textSizeMult

    Text(
        stringResource(R.string.strPreviewTextTransl),
        fontSize = fontSize,
        lineHeight = fontSize * 1.5,
    )
}
