package com.quranapp.android.compose.components.reader

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.quran.AyahWordEntity

@Composable
fun QuranTextWbw(
    verseUi: ReaderLayoutItem.VerseUI,
    onWordClick: ((AyahWordEntity) -> Unit)?
) {
    val wbwMap = verseUi.wbwByWordIndex ?: emptyMap()
    val textStyles = LocalQuranTextStyle.current
    val recitation = LocalRecitation.current

    val arabicStyle = textStyles.quran(verseUi.verse.pageNo) ?: TextStyle.Default
    val dividerColor = colorScheme.outlineVariant

    fun handleWordClick(word: AyahWordEntity) {
        if (onWordClick != null) {
            onWordClick(word)
        } else if (ReaderPreferences.getWbwRecitationEnabled() && !word.isLastWordOfAyah) {
            recitation.playWord(
                verseUi.verse.chapterNo,
                verseUi.verse.verseNo,
                word.wordIndex
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (word in verseUi.verse.words) {
                if (word.isLastWordOfAyah) {
                    Text(
                        text = word.text,
                        style = arabicStyle,
                        modifier = Modifier
                            .clickable {
                                handleWordClick(word)
                            },
                    )
                } else {
                    val wbw = wbwMap[word.wordIndex]
                    val isThisWordLoading = recitation.isWbwAudioLoading(
                        verseUi.verse.chapterNo,
                        verseUi.verse.verseNo,
                        word.wordIndex
                    )

                    Column(
                        modifier = Modifier
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .clickable {
                                handleWordClick(word)
                            },
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
        }
    }
}
