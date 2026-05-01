package com.quranapp.android.compose.components.reader.dialogs

import com.quranapp.android.compose.utils.ThemeUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alfaazplus.sunnah.ui.theme.tightTextStyle
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.components.reader.LocalReaderViewModel
import com.quranapp.android.compose.extensions.bottomBorder
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.extras.SimilarVerseEntity
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.TranslationTextStyleParams
import com.quranapp.android.utils.reader.buildTranslationAnnotatedString
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.utils.reader.getTranslationTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException

private data class SimilarVerseListRow(
    val entity: SimilarVerseEntity,
    val surahName: String,
    val surahNo: Int,
    val verseNo: Int,
    val words: List<AyahWordEntity>,
    val translation: Translation?,
    val highlightWordIndices: Set<Int>,
    val pageNo: Int,
)

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
)
@Composable
fun SimilarVersesSheet(
    sourceVerse: VerseWithDetails?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val viewModel = LocalReaderViewModel.current
    val fontResolver = viewModel.fontResolver
    val repository = viewModel.repository

    val arabicMult = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val scriptCode = ReaderPreferences.observeQuranScript()
    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()
    val isDark = ThemeUtils.observeDarkTheme()
    val primarySlug = ReaderPreferences.observePrimaryTranslationSlug()

    var rows by remember { mutableStateOf<List<SimilarVerseListRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(sourceVerse, scriptCode, primarySlug) {
        val v = sourceVerse

        if (v == null) {
            rows = emptyList()
            loading = false
            return@LaunchedEffect
        }

        loading = true

        rows = withContext(Dispatchers.IO) {
            val similar = repository.extrasDao.getSimilarVerses(v.id)

            QuranTranslationFactory(context).use { factory ->
                similar.mapNotNull { s ->
                    val ayah = repository.getAyahById(s.matchedAyahId)
                        ?: return@mapNotNull null
                    val surah = repository.getSurahWithLocalizations(ayah.surahNo)
                        ?: return@mapNotNull null

                    val vwd = repository.getVerseWithDetails(ayah.surahNo, ayah.ayahNo, scriptCode, arabicEnabled)
                        ?: return@mapNotNull null

                    val translation = if (
                        primarySlug.isNotEmpty() &&
                        factory.isTranslationDownloaded(primarySlug)
                    ) {
                        val list = factory.getTranslationsSingleVerse(
                            setOf(primarySlug),
                            ayah.surahNo,
                            ayah.ayahNo,
                        )
                        list.firstOrNull()
                    } else {
                        null
                    }

                    val highlights = parseMatchWordIndices(s.matchWords)

                    SimilarVerseListRow(
                        entity = s,
                        surahName = surah.getCurrentName(),
                        surahNo = ayah.surahNo,
                        verseNo = ayah.ayahNo,
                        words = vwd.words,
                        translation = translation,
                        highlightWordIndices = highlights,
                        pageNo = vwd.pageNo,
                    )
                }
            }
        }

        loading = false
    }

    if (sourceVerse == null) {
        return
    }

    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)

    val quranTextStyle = remember(
        sourceVerse,
        scriptCode,
        arabicMult,
        fontResolver,
        isDark,
    ) {
        { pageNo: Int ->
            getQuranTextStyle(
                QuranTextStyleParams(
                    context = context,
                    fontResolver = fontResolver,
                    colors = colors,
                    type = type,
                    pageNo = pageNo,
                    script = scriptCode,
                    sizeMultiplier = arabicMult,
                    useSmallSize = true,
                    isDark = isDark,
                ),
            )
        }
    }

    val translationTextStyle = remember(
        translMult,
        primarySlug,
    ) {
        getTranslationTextStyle(
            TranslationTextStyleParams(
                slug = primarySlug,
                type = type,
                sizeMultiplier = translMult,
            ),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Header(sourceVerse, rows.size)

            if (loading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(
                        count = rows.size,
                        key = { index -> "${rows[index].entity.sourceAyahId}-${rows[index].entity.matchedAyahId}" },
                    ) { index ->
                        val row = rows[index]

                        SimilarVerseItem(
                            row = row,
                            arabicStyle = quranTextStyle(row.pageNo),
                            translationStyle = translationTextStyle,
                            onOpenInReader = {
                                ReaderFactory.startVerse(context, row.surahNo, row.verseNo)
                                onDismiss()
                            },
                        )

                        if (index < rows.lastIndex) {
                            HorizontalDivider(
                                color = colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun Header(verse: VerseWithDetails, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bottomBorder()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            stringResource(R.string.similarVerses),
            style = typography.titleMedium.merge(tightTextStyle),
        )

        Text(
            text = stringResource(
                R.string.countSimilarVerses,
                stringResource(
                    R.string.strLabelVerseWithChapNameAndNo,
                    verse.chapter.getCurrentName(),
                    verse.chapterNo,
                    verse.verseNo
                ),
                count,
            ),
            style = typography.labelMedium,
            color = colorScheme.onSurface.alpha(0.8f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimilarVerseItem(
    row: SimilarVerseListRow,
    arabicStyle: TextStyle,
    translationStyle: TextStyle,
    onOpenInReader: () -> Unit,
) {
    val translation = row.translation
    val isDark = ThemeUtils.observeDarkTheme()
    val transColor = if (isDark) colorScheme.onBackground.alpha(0.88f) else colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(
                        R.string.strLabelVerseSerialWithChapter,
                        row.surahName,
                        row.surahNo,
                        row.verseNo,
                    ),
                    style = typography.labelLarge.merge(tightTextStyle),
                    color = colorScheme.primary,
                )

                Text(
                    text = stringResource(
                        R.string.similarVerseRowMeta,
                        row.entity.coverage,
                        row.entity.score,
                    ),
                    style = typography.bodyMedium.merge(tightTextStyle),
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            TextButton(
                onClick = onOpenInReader,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.surfaceContainerHigh,
                    contentColor = colorScheme.onSurface.alpha(0.8f),
                ),
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = shapes.small
            ) {
                Text(
                    text = stringResource(R.string.strLabelOpenInReader),
                    style = typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val ordered = row.words.sortedBy { it.wordIndex }
                for (word in ordered) {
                    val highlight = word.wordIndex in row.highlightWordIndices

                    val bgMod = if (highlight) {
                        Modifier.background(
                            color = colorScheme.primary.alpha(0.4f),
                        )
                    } else {
                        Modifier
                    }

                    Text(
                        text = word.text,
                        style = arabicStyle,
                        modifier = bgMod.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }

        if (translation != null) {
            val annotated = buildTranslationAnnotatedString(
                translation = translation,
                colorScheme = colorScheme,
                actions = null,
            )

            SelectionContainer {
                Text(
                    text = annotated,
                    style = translationStyle,
                    color = transColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        }
    }
}


private fun parseMatchWordIndices(json: String): Set<Int> {
    if (json.isBlank()) return emptySet()

    return try {
        val out = mutableSetOf<Int>()
        val outer = JSONArray(json)

        for (i in 0 until outer.length()) {
            val pair = outer.getJSONArray(i)
            val from = pair.getInt(0)
            val to = pair.getInt(1)
            for (w in from..to) out.add(w)
        }

        out
    } catch (_: JSONException) {
        emptySet()
    }
}
