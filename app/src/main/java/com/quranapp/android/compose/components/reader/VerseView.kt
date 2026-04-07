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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory

data class LocalRecitationStateData(
    val controller: RecitationController,
    val isAnyPlaying: Boolean,
    val playingVerse: ChapterVersePair,
    val onVerseRecitationStarted: (() -> Unit) = {}
)

val LocalRecitationState = staticCompositionLocalOf<LocalRecitationStateData> {
    error("LocalRecitationState not provided")
}

@Composable
fun VerseView(
    verseUi: ReaderLayoutItem.VerseUI,
    isBookmarked: Boolean,
    showDivider: Boolean = false
) {
    val verse = verseUi.verse

    val recState = LocalRecitationState.current
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
            QuranText(verseUi = verseUi)
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
    verse: Verse,
    isVersePlaying: Boolean,
    isBookmarked: Boolean
) {
    val context = LocalContext.current
    val verseActions = LocalVerseActions.current
    val recitationState = LocalRecitationState.current
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

            VerseActionIconButton(
                painter = painterResource(if (isVersePlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = stringResource(R.string.strTitleVerseRecitation),
                tint = if (isVersePlaying) colorScheme.primary else iconTint
            ) {
                if (isVersePlaying) {
                    controller.pause()
                } else {
                    controller.start(ChapterVersePair(verse))
                    recitationState.onVerseRecitationStarted()
                }
            }

            VerseActionIconButton(
                painter = painterResource(R.drawable.dr_icon_tafsir),
                contentDescription = stringResource(R.string.strTitleTafsir),
                tint = Color.Unspecified,
            ) { ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo) }

            VerseActionIconButton(
                painter = painterResource(
                    if (isBookmarked) R.drawable.dr_icon_bookmark_added
                    else R.drawable.dr_icon_bookmark_outlined
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

@Composable
private fun VerseActionIconButton(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    tint: Color,
    onClick: () -> Unit,
) {
    SimpleTooltip(contentDescription) {
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
private fun VerseSerial(verse: Verse) {
    val context = LocalContext.current
    val quranMeta = QuranMeta2.remember()

    val chapterNo = verse.chapterNo
    val verseNo = verse.verseNo

    if (quranMeta == null) return

    val contentDescription = stringResource(
        R.string.strDescVerseNoWithChapter,
        quranMeta.getChapterName(context, chapterNo),
        verseNo
    )

    Text(
        text = if (verse.includeChapterNameInSerial) {
            stringResource(
                R.string.strLabelVerseSerialWithChapter,
                quranMeta.getChapterName(context, chapterNo),
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
            .background(colorResource(R.color.colorBGLightGrey))
            .clickable(
                onClick = {
                    context.copyToClipboard(verse.id.toString())
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = colorResource(R.color.colorIcon),
        style = typography.labelLarge,
    )
}
