package com.quranapp.android.compose.components.reader.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.reader.LocalQuranTextStyle
import com.quranapp.android.compose.components.reader.LocalReaderViewModel
import com.quranapp.android.compose.components.reader.LocalWbwState
import com.quranapp.android.compose.components.reader.LocalWbwStateData
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.TextStyleProvider
import com.quranapp.android.compose.components.reader.VerseView
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.UserRepository
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.ReaderProviderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WbwSheetData(
    val chapterNo: Int,
    val verseNo: Int,
    val wordIndex: Int
)

private data class WordInfoContent(
    val verseUi: ReaderLayoutItem.VerseUI,
    val textStyles: Map<Int, TextStyle>,
    val word: AyahWordEntity,
    val wbwWord: WbwWordEntity?,
    val chapterName: String,
    val prev: WbwSheetData?,
    val next: WbwSheetData?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WbwSheet(
    data: WbwSheetData?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (data == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
    ) {
        Content(data)
    }
}

@Composable
private fun Content(data: WbwSheetData) {
    var currentData by remember { mutableStateOf(data) }

    val context = LocalContext.current
    val colors = colorScheme
    val type = typography
    val vm = LocalReaderViewModel.current
    val verseActions = LocalVerseActions.current

    val content by produceState<WordInfoContent?>(
        null,
        currentData,
        context,
        colors,
        type,
        verseActions
    ) {
        withContext(Dispatchers.IO) {
            val script = ReaderPreferences.getQuranScript()
            val wbwId = ReaderPreferences.getWbwId()

            val words = vm.repository.getWordsForAyah(
                currentData.chapterNo,
                currentData.verseNo,
                script
            )

            val theWord = words[currentData.wordIndex]

            val wbwRow = wbwId?.let {
                val map =
                    vm.repository.getWbwWordsForAyahs(
                        wbwId = wbwId,
                        ayahIds = listOf(theWord.ayahId),
                        wbwTranslation = true,
                        wbwTransliteration = true,
                    )
                map[theWord.ayahId]?.get(theWord.wordIndex)
            }

            val chapterNo = currentData.chapterNo
            val verseNo = currentData.verseNo
            val wordIndex = currentData.wordIndex
            val verseCount = vm.repository.getChapterVerseCount(chapterNo)

            val prev = when {
                wordIndex > 0 -> WbwSheetData(chapterNo, verseNo, wordIndex - 1)
                verseNo > 1 -> {
                    val prevWords = vm.repository.getWordsForAyah(chapterNo, verseNo - 1, script)
                    if (prevWords.isEmpty()) null
                    else WbwSheetData(chapterNo, verseNo - 1, prevWords.lastIndex)
                }

                chapterNo > 1 -> {
                    val prevCh = chapterNo - 1
                    val lastVerse = vm.repository.getChapterVerseCount(prevCh)
                    if (lastVerse <= 0) null
                    else {
                        val prevWords = vm.repository.getWordsForAyah(prevCh, lastVerse, script)
                        if (prevWords.isEmpty()) null
                        else WbwSheetData(prevCh, lastVerse, prevWords.lastIndex)
                    }
                }

                else -> null
            }

            val next = when {
                wordIndex < words.lastIndex -> WbwSheetData(chapterNo, verseNo, wordIndex + 1)
                verseNo < verseCount -> {
                    val nextWords = vm.repository.getWordsForAyah(chapterNo, verseNo + 1, script)
                    if (nextWords.isEmpty()) null
                    else WbwSheetData(chapterNo, verseNo + 1, 0)
                }

                chapterNo < QuranMeta.chapterRange.last -> {
                    val nextWords = vm.repository.getWordsForAyah(chapterNo + 1, 1, script)
                    if (nextWords.isEmpty()) null
                    else WbwSheetData(chapterNo + 1, 1, 0)
                }

                else -> null
            }

            val prepared = ReaderItemsBuilder.buildQuickReferenceItems(
                context,
                params = TextBuilderParams(
                    context = context,
                    fontResolver = vm.fontResolver,
                    verseActions = verseActions,
                    colors = colors,
                    type = type,
                    arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
                    script = ReaderPreferences.getQuranScript(),
                    arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
                    translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
                    slugs = ReaderPreferences.getTranslations(),
                ),
                repository = vm.repository,
                chapterNo = chapterNo,
                verseNos = listOf(currentData.verseNo)
            )

            val verseRows = prepared?.items.orEmpty().filterIsInstance<ReaderLayoutItem.VerseUI>()
            if (prepared == null || verseRows.isEmpty()) return@withContext

            value = WordInfoContent(
                verseUi = verseRows.first(),
                textStyles = prepared.textStyles,
                word = theWord,
                wbwWord = wbwRow,
                chapterName = vm.repository.getChapterName(currentData.chapterNo),
                prev = prev,
                next = next,
            )
        }
    }

    if (content == null) {
        return Loader()
    }

    TextStyleProvider(
        content!!.textStyles
    ) {
        WordContent(vm, content!!) {
            currentData = it
        }
    }
}

@Composable
private fun WordContent(
    vm: ReaderProviderViewModel,
    content: WordInfoContent,
    onWordChange: (WbwSheetData) -> Unit
) {
    val word = content.word
    val wbwRow = content.wbwWord
    val verseUi = content.verseUi
    val chapterNo = verseUi.verse.chapterNo
    val verseNo = verseUi.verse.verseNo

    val wbwRecitationEnabled = ReaderPreferences.observeWbwRecitationEnabled()
    val copyScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current

    val wbwState = LocalWbwState.current

    LaunchedEffect(word.ayahId, word.wordIndex, wbwRecitationEnabled) {
        if (word.isLastWordOfAyah) return@LaunchedEffect

        wbwState.onWordClick(word)
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VerseContextHeader(
            chapterName = content.chapterName,
            chapterNo = chapterNo,
            verseNo = verseNo,
            word = word,
            onCopyWord = {
                copyScope.launch {
                    val text = resolveWordTextForCopy(
                        vm.repository,
                        chapterNo,
                        verseNo,
                        word,
                    )

                    context.copyToClipboard(text)

                    MessageUtils.showClipboardMessage(
                        context,
                        resources.getString(R.string.copiedToClipboard),
                    )
                }
            },
            onPlayWord = {
                wbwState.onForcePlay(word)
            }
        )

        ArabicWordCard(
            wbwState = wbwState,
            word = word,
            wbwRow = wbwRow,
            textStyle = content.textStyles.get(verseUi.verse.pageNo) ?: TextStyle.Default,
            hasPrev = content.prev != null,
            hasNext = content.next != null,
            onPrev = {
                if (content.prev != null) onWordChange(content.prev)
            },
            onNext = {
                if (content.next != null) onWordChange(content.next)
            },
        )


        HorizontalDivider(
            modifier = Modifier.padding(top = 24.dp),
            color = colorScheme.outlineVariant,
        )

        VerseWrapped(
            bookmarksRepo = vm.userRepository,
            verseUi = verseUi,
            onWordClick = {
                val pair = QuranUtils.getVerseNoFromAyahId(it.ayahId)
                onWordChange(
                    WbwSheetData(
                        pair.first,
                        pair.second,
                        it.wordIndex
                    )
                )
            }
        )
    }
}

@Composable
private fun VerseContextHeader(
    chapterName: String,
    chapterNo: Int,
    verseNo: Int,
    word: AyahWordEntity,
    onCopyWord: () -> Unit,
    onPlayWord: () -> Unit,
) {
    val parts = buildList {
        if (chapterName.isNotBlank()) add(chapterName)
        add(String.format($$"%1$d:%2$d", chapterNo, verseNo))
        add(stringResource(R.string.wordNo, word.wordIndex + 1))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = parts.joinToString("  ·  "),
            style = typography.labelLarge,
            color = colorScheme.onSurfaceVariant.alpha(0.8f),
            modifier = Modifier.weight(1f)
        )

        if (!word.isLastWordOfAyah) {
            IconButton(
                painterResource(R.drawable.ic_play),
                contentDescription = stringResource(R.string.playWord),
                onClick = onPlayWord,
                tint = colorScheme.onSurfaceVariant.alpha(0.8f),
                small = true
            )
        }

        IconButton(
            painterResource(R.drawable.icon_copy),
            contentDescription = stringResource(R.string.strLabelCopy),
            onClick = onCopyWord,
            tint = colorScheme.onSurfaceVariant.alpha(0.8f),
            small = true
        )
    }
}

@Composable
private fun ArabicWordCard(
    word: AyahWordEntity,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    textStyle: TextStyle,
    wbwRow: WbwWordEntity?,
    wbwState: LocalWbwStateData,
) {
    val transliteration = wbwRow?.transliteration?.takeIf { !it.isNullOrBlank() }
    val translation = wbwRow?.translation?.takeIf { !it.isNullOrBlank() }
    val textStyles = LocalQuranTextStyle.current

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant.alpha(0.4f))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        painter = painterResource(R.drawable.dr_icon_chevron_left),
                        contentDescription = stringResource(R.string.previousWord),
                        onClick = onPrev,
                        enabled = hasPrev,
                        tint = colorScheme.onSurfaceVariant.alpha(if (hasPrev) 0.9f else 0.35f),
                        small = true,
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = word.text,
                            style = textStyle.copy(
                                fontSize = 40.sp
                            ),
                            color = colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(
                        painter = painterResource(R.drawable.dr_icon_chevron_right),
                        contentDescription = stringResource(R.string.nextWord),
                        onClick = onNext,
                        enabled = hasNext,
                        tint = colorScheme.onSurfaceVariant.alpha(if (hasNext) 0.9f else 0.35f),
                        small = true,
                    )
                }
            }

            if (translation != null || transliteration != null) {
                Spacer(Modifier.height(16.dp))

                CompositionLocalProvider(LocalLayoutDirection provides if (wbwState.isWbwRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    if (transliteration != null) {
                        Text(
                            transliteration,
                            style = textStyles.wbwTrltStyle ?: TextStyle.Default
                        )
                    }

                    if (translation != null) {
                        Text(
                            translation,
                            style = textStyles.wbwTrStyle ?: TextStyle.Default
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun VerseWrapped(
    bookmarksRepo: UserRepository,
    verseUi: ReaderLayoutItem.VerseUI,
    onWordClick: (AyahWordEntity) -> Unit
) {
    val verse = verseUi.verse

    val isBookmarked by bookmarksRepo
        .isBookmarkedFlow(verse.chapterNo, verse.verseNo..verse.verseNo)
        .collectAsStateWithLifecycle(false)

    VerseView(
        verseUi = verseUi,
        isBookmarked = isBookmarked,
        showDivider = false,
        onWordClick = onWordClick
    )
}

private suspend fun resolveWordTextForCopy(
    repository: QuranRepository,
    chapterNo: Int,
    verseNo: Int,
    word: AyahWordEntity,
): String {
    val script = ReaderPreferences.getQuranScript()

    if (script == QuranScriptUtils.SCRIPT_UTHMANI) return word.text

    return withContext(Dispatchers.IO) {
        val uthmaniWords = repository.getWordsForAyah(
            chapterNo,
            verseNo,
            QuranScriptUtils.SCRIPT_UTHMANI,
        )

        uthmaniWords.find { it.wordIndex == word.wordIndex }?.text ?: word.text
    }
}