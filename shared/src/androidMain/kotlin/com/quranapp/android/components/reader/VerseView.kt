package com.quranapp.android.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.components.common.dialogs.SimpleTooltipPosition
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_menu
import com.quranapp.android.generated.resources.ic_book_open_text
import com.quranapp.android.generated.resources.ic_bookmark
import com.quranapp.android.generated.resources.ic_bookmark_added
import com.quranapp.android.generated.resources.ic_pause
import com.quranapp.android.generated.resources.ic_play
import com.quranapp.android.generated.resources.strDescVerseNoWithChapter
import com.quranapp.android.generated.resources.strLabelBookmark
import com.quranapp.android.generated.resources.strLabelVerseSerial
import com.quranapp.android.generated.resources.strLabelVerseSerialWithChapter
import com.quranapp.android.generated.resources.strTitleTafsir
import com.quranapp.android.generated.resources.strTitleVerseOptions
import com.quranapp.android.generated.resources.strTitleVerseRecitation
import com.quranapp.android.locale.formattedStringResource
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.reader.ReaderLayoutItem
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val VERSE_HIGHLIGHT_ALPHA = 0.28f
const val VERSE_WORD_HIGHLIGHT_ALPHA = 0.38f
const val VBV_VERSE_HIGHLIGHT_ALPHA = 0.2f

enum class VerseActionBarMode {
    Full,
    Minimal,
}

@Composable
fun VerseView(
    verseUi: ReaderLayoutItem.VerseUI,
    isBookmarked: Boolean,
    showDivider: Boolean = false,
    onWordClick: ((AyahWordEntity) -> Unit)? = null,
    actionBarMode: VerseActionBarMode = VerseActionBarMode.Full,
    onPlayClick: (() -> Unit)? = null,
) {
    val verse = verseUi.verse

    val recState = LocalRecitation.current
    val isVersePlaying by remember(verse, recState) {
        derivedStateOf {
            recState.isAnyPlayingState.value && recState.playingVerseState.value.doesEqual(verse)
        }
    }

    val highlightState = LocalTransientVerseHighlight.current
    val highlightAlpha = highlightState.alpha
    val isHighlighted = highlightState.verse?.let {
        it.chapterNo == verse.chapterNo && it.verseNo == verse.verseNo
    } == true

    val colors = colorScheme

    Box {
        Column(
            modifier = Modifier
                .drawBehind {
                    if (isVersePlaying || isHighlighted) {
                        val alpha = if (isHighlighted) highlightAlpha.value else 1f
                        drawRect(colors.primary.copy(alpha = VBV_VERSE_HIGHLIGHT_ALPHA * alpha))
                    }
                }
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            VerseActionBar(verse, isVersePlaying, isBookmarked, actionBarMode, onPlayClick)

            QuranTextWbw(
                verseUi = verseUi,
                onWordClick = onWordClick
            )

            TranslationText(verseUi = verseUi)
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = 1.dp,
                color = colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun VerseActionBar(
    verse: VerseWithDetails,
    isVersePlaying: Boolean,
    isBookmarked: Boolean,
    actionBarMode: VerseActionBarMode,
    onPlayClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val verseActions = LocalVerseActions.current
    val recitationState = LocalRecitation.current
    val controller = recitationState.controller

    val iconTint = colorScheme.onBackground.alpha(0.7f)
    val bookmarkTint = if (isBookmarked) colorScheme.primary else iconTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (actionBarMode == VerseActionBarMode.Full) {
                VerseActionIconButton(
                    painter = painterResource(Res.drawable.dr_icon_menu),
                    contentDescription = stringResource(Res.string.strTitleVerseOptions),
                    tint = iconTint,
                    iconModifier = Modifier.rotate(90f),
                ) {
                    verseActions.onVerseOption?.invoke(verse)
                }
            }

            PlayControlButton(controller, isVersePlaying, iconTint, verse, onPlayClick)

            VerseActionIconButton(
                painter = painterResource(Res.drawable.ic_book_open_text),
                contentDescription = stringResource(Res.string.strTitleTafsir),
                tint = iconTint,
            ) { ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo) }

            if (actionBarMode == VerseActionBarMode.Full) {
                VerseActionIconButton(
                    painter = painterResource(
                        if (isBookmarked) Res.drawable.ic_bookmark_added
                        else Res.drawable.ic_bookmark
                    ),
                    contentDescription = stringResource(Res.string.strLabelBookmark),
                    tint = bookmarkTint,
                ) {
                    verseActions.onBookmarkRequest?.invoke(
                        verse.chapterNo,
                        verse.verseNo..verse.verseNo
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        VerseSerial(
            verse = verse,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayControlButton(
    controller: RecitationController,
    isVersePlaying: Boolean,
    iconTint: Color,
    verse: VerseWithDetails,
    onPlayClick: (() -> Unit)? = null,
) {
    val label = stringResource(Res.string.strTitleVerseRecitation)

    SimpleTooltip(
        text = label,
        position = SimpleTooltipPosition.Above
    ) {
        Box(
            modifier = Modifier
                .semantics {
                    this.contentDescription = label
                }
                .size(32.dp)
                .clip(CircleShape)
                .clickable {
                    if (onPlayClick != null) {
                        onPlayClick()
                    } else {
                        controller.playControl(ChapterVersePair(verse))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isVersePlaying) Res.drawable.ic_pause else Res.drawable.ic_play),
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp),
                tint = iconTint,
            )
        }
    }
}

@Composable
fun VerseActionIconButton(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    tint: Color,
    onClick: () -> Unit,
) {
    SimpleTooltip(contentDescription, position = SimpleTooltipPosition.Above) {
        Box(
            modifier = modifier
                .semantics { this.contentDescription = contentDescription }
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp)
                    .then(iconModifier),
                tint = tint,
            )
        }
    }
}

@Composable
fun VerseSerial(
    verse: VerseWithDetails,
) {
    val context = LocalContext.current
    val chapterNo = verse.chapterNo
    val verseNo = verse.verseNo

    val contentDescription = formattedStringResource(
        Res.string.strDescVerseNoWithChapter,
        verse.chapter.getCurrentName(),
        verseNo
    )

    Text(
        text = if (verse.includeChapterNameInSerial) {
            formattedStringResource(
                Res.string.strLabelVerseSerialWithChapter,
                verse.chapter.getCurrentName(),
                chapterNo,
                verseNo
            )
        } else formattedStringResource(
            Res.string.strLabelVerseSerial,
            chapterNo,
            verseNo
        ),
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(5.dp))
            .background(colorScheme.surfaceContainerLow)
            .clickable { context.copyToClipboard(contentDescription) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = colorScheme.onSurface,
        style = typography.labelLarge,
    )
}
