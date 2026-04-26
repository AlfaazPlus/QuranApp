package com.quranapp.android.compose.components.reader

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.dialogs.SimpleTooltipPosition
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory

@Composable
fun VerseView(
    verseUi: ReaderLayoutItem.VerseUI,
    isBookmarked: Boolean,
    showDivider: Boolean = false,
    onWordClick: ((AyahWordEntity) -> Unit)? = null,
) {
    val verse = verseUi.verse

    val recState = LocalRecitation.current
    val isVersePlaying = recState.isAnyPlaying && recState.playingVerse.doesEqual(verse)

    Box {
        Column(
            modifier = Modifier
                .background(
                    if (isVersePlaying) colorScheme.primary.alpha(0.2f)
                    else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            VerseActionBar(verse = verse, isVersePlaying, isBookmarked)

            QuranTextWbw(
                verseUi,
                onWordClick
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
    isBookmarked: Boolean
) {
    val context = LocalContext.current
    val verseActions = LocalVerseActions.current
    val recitationState = LocalRecitation.current
    val controller = recitationState.controller

    val iconTint = colorScheme.onBackground.alpha(0.7f)
    val bookmarkTint = if (isBookmarked) colorResource(R.color.colorPrimary) else iconTint

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
            VerseActionIconButton(
                painter = painterResource(R.drawable.dr_icon_menu),
                contentDescription = stringResource(R.string.strTitleVerseOptions),
                tint = iconTint,
                iconModifier = Modifier.rotate(90f),
            ) {
                verseActions.onVerseOption?.invoke(verse)
            }

            PlayControlButton(controller, isVersePlaying, iconTint, verse)

            VerseActionIconButton(
                painter = painterResource(R.drawable.dr_icon_tafsir),
                contentDescription = stringResource(R.string.strTitleTafsir),
                tint = Color.Unspecified,
            ) { ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo) }

            VerseActionIconButton(
                painter = painterResource(
                    if (isBookmarked) R.drawable.ic_bookmark_added
                    else R.drawable.ic_bookmark
                ),
                contentDescription = stringResource(R.string.strLabelBookmark),
                tint = bookmarkTint,
            ) {
                verseActions.onBookmarkRequest?.invoke(
                    verse.chapterNo,
                    verse.verseNo..verse.verseNo
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        VerseSerial(verse = verse)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayControlButton(
    controller: RecitationController,
    isVersePlaying: Boolean,
    iconTint: Color,
    verse: VerseWithDetails
) {
    val label = stringResource(R.string.strTitleVerseRecitation)

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
                    controller.playControl(ChapterVersePair(verse))
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isVersePlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp),
                tint = iconTint,
            )
        }
    }
}

@Composable
private fun VerseActionIconButton(
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
private fun VerseSerial(verse: VerseWithDetails) {
    val context = LocalContext.current
    val chapterNo = verse.chapterNo
    val verseNo = verse.verseNo


    val contentDescription = stringResource(
        R.string.strDescVerseNoWithChapter,
        verse.chapter.getCurrentName(),
        verseNo
    )

    Text(
        text = if (verse.includeChapterNameInSerial) {
            stringResource(
                R.string.strLabelVerseSerialWithChapter,
                verse.chapter.getCurrentName(),
                chapterNo,
                verseNo
            )
        } else stringResource(
            R.string.strLabelVerseSerial,
            chapterNo,
            verseNo
        ),
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(5.dp))
            .background(colorScheme.surface)
            .clickable(
                onClick = {
                    context.copyToClipboard(verse.id.toString())
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = colorScheme.onSurface,
        style = typography.labelLarge,
    )
}
