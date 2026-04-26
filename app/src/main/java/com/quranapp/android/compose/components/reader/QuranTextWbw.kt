package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.components.reader.dialogs.WbwSheetData
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.wbw.WbwWordEntity

@Composable
fun QuranTextWbw(
    verseUi: ReaderLayoutItem.VerseUI,
    onWordClick: ((AyahWordEntity) -> Unit)?
) {
    val wbwMap = verseUi.wbwByWordIndex ?: emptyMap()
    val textStyles = LocalQuranTextStyle.current
    val wbwState = LocalWbwState.current

    val withinWbwSheet = onWordClick != null
    val shouldShowTooltip = !withinWbwSheet && !wbwState.isWbwSheetOpen

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (word in verseUi.verse.words) {
                val wbwForWord = wbwMap[word.wordIndex]

                if (wbwState.activeTooltipWord == word && shouldShowTooltip) {
                    WbwTooltip(
                        word = word,
                        onDismiss = { wbwState.onDismissTooltip() },
                        onOpenSheet = {
                            wbwState.toggleWbwSheet(
                                WbwSheetData(
                                    chapterNo = verseUi.verse.chapterNo,
                                    verseNo = verseUi.verse.verseNo,
                                    wordIndex = word.wordIndex
                                )
                            )
                        },
                        textStyles = textStyles,
                    ) {
                        QuranTextWbwWordCell(
                            active = true,
                            word = word,
                            verseUi = verseUi,
                            wbw = wbwForWord,
                            textStyles = textStyles,
                            wbwState = wbwState,
                            onWordClick = onWordClick,
                        )
                    }
                } else {
                    QuranTextWbwWordCell(
                        active = wbwState.activeTooltipWord == word,
                        word = word,
                        verseUi = verseUi,
                        wbw = wbwForWord,
                        textStyles = textStyles,
                        wbwState = wbwState,
                        onWordClick = onWordClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranTextWbwWordCell(
    active: Boolean,
    word: AyahWordEntity,
    verseUi: ReaderLayoutItem.VerseUI,
    wbw: WbwWordEntity?,
    textStyles: QuranTextStyle,
    wbwState: LocalWbwStateData,
    onWordClick: ((AyahWordEntity) -> Unit)?,
) {
    val arabicStyle = textStyles.quran(verseUi.verse.pageNo) ?: TextStyle.Default

    val dividerColor = colorScheme.outlineVariant

    fun handleWordClick(word: AyahWordEntity) {
        if (onWordClick != null) {
            onWordClick(word)
        } else if (!word.isLastWordOfAyah) {
            wbwState.onWordClick(word)
        }
    }

    if (word.isLastWordOfAyah) {
        Text(
            text = word.text,
            style = arabicStyle,
            modifier = Modifier.clickable { handleWordClick(word) },
        )
    } else {
        val isThisWordLoading = wbwState.isWbwAudioLoading(
            verseUi.verse.chapterNo,
            verseUi.verse.verseNo,
            word.wordIndex
        )

        Column(
            modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .background(
                    if (active) colorScheme.primary.alpha(0.3f) else Color.Transparent,
                    shape = shapes.small
                )
                .clickable { handleWordClick(word) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val hasTransliteration = !wbw?.transliteration.isNullOrBlank()
            val hasTranslation = !wbw?.translation.isNullOrBlank()
            val showDividerUnderArabic = hasTransliteration || hasTranslation

            if (isThisWordLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .widthIn(min = 28.dp, max = 40.dp)
                        .height(2.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                )
            }

            Text(
                text = word.text,
                style = arabicStyle,
                modifier = if (showDividerUnderArabic) {
                    Modifier.drawBehind {
                        val stroke = 1.dp.toPx()
                        val y = size.height + stroke * 0.5f
                        drawLine(
                            color = dividerColor,
                            strokeWidth = stroke,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                        )
                    }
                } else {
                    Modifier
                },
            )

            Spacer(Modifier.height(3.dp))

            if (hasTransliteration) {
                Text(
                    text = wbw.transliteration,
                    style = textStyles.wbwTrltStyle ?: TextStyle.Default,
                    textAlign = TextAlign.Center,
                )
            }

            if (hasTranslation) {
                Text(
                    text = wbw.translation,
                    style = textStyles.wbwTrStyle ?: TextStyle.Default,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = textStyles.wbwMaxWith),
                )
            }
        }
    }
}
