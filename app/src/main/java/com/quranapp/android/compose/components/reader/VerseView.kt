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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.VerseViewModel

@Composable
fun VerseView(
    verse: Verse,
    slugs: Set<String>,
    showDivider: Boolean = false
) {
    val viewmodel = viewModel<VerseViewModel>()
    val controller = viewmodel.controller
    val scrollSync = RecitationPreferences.observeScrollSync()

    val state by controller.state.collectAsState()
    val isPlaying by controller.isPlayingState.collectAsState()
    val isVersePlaying = isPlaying && state.isCurrentVerse(verse.chapterNo, verse.verseNo)

    Box() {
        Column(
            modifier = Modifier
                .background(
                    if (isVersePlaying && scrollSync) colorScheme.primary.alpha(0.2f)
                    else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            VerseActionBar(verse = verse, controller, isVersePlaying)
            QuranText(verse)
            TranslationText(
                verse = verse,
                slugs,
            )

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
    controller: RecitationController,
    isVersePlaying: Boolean
) {
    val context = LocalContext.current
    val verseActions = LocalVerseActions.current

    var isBookmarked = false

    val iconTint = colorScheme.onBackground.alpha(0.7f)
    val bookmarkTint = if (isBookmarked) colorResource(R.color.colorPrimary) else iconTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerseActionIconButton(
                onClick = {
                    verseActions.onVerseOption?.invoke(verse)
                },
                painter = painterResource(R.drawable.dr_icon_menu),
                contentDescription = stringResource(R.string.strTitleVerseOptions),
                tint = iconTint,
                iconModifier = Modifier.rotate(90f),
            )

            VerseActionIconButton(
                onClick = {
                    if (isVersePlaying) {
                        controller.pause()
                    } else {
                        controller.start(ChapterVersePair(verse))
                    }
                },
                painter = painterResource(if (isVersePlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = stringResource(R.string.strTitleVerseRecitation),
                tint = if (isVersePlaying) colorScheme.primary else iconTint
            )

            VerseActionIconButton(
                onClick = { ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo) },
                painter = painterResource(R.drawable.dr_icon_tafsir),
                contentDescription = stringResource(R.string.strTitleTafsir),
                tint = Color.Unspecified,
            )

            VerseActionIconButton(
                onClick = {
                    // TODO
                },
                painter = painterResource(
                    if (isBookmarked) R.drawable.dr_icon_bookmark_added
                    else R.drawable.dr_icon_bookmark_outlined
                ),
                contentDescription = stringResource(R.string.strLabelBookmark),
                tint = bookmarkTint,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        VerseSerial(verse = verse)
    }
}

@Composable
private fun VerseActionIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    tint: Color = colorResource(R.color.colorIcon2),
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(6.dp)
                .then(iconModifier),
            tint = tint,
        )
    }
}

@Composable
private fun VerseSerial(verse: Verse) {
    val context = LocalContext.current
    val quranMeta = QuranMeta2.rememberQuranMeta()

    val chapterNo = verse.chapterNo
    val verseNo = verse.verseNo

    val (label, serialContentDescription) = remember(
        verse.includeChapterNameInSerial,
        chapterNo,
        verseNo,
        quranMeta,
    ) {
        if (verse.includeChapterNameInSerial && quranMeta != null) {
            val name = quranMeta.getChapterName(context, chapterNo)
            context.getString(
                R.string.strLabelVerseSerialWithChapter,
                name,
                chapterNo,
                verseNo
            ) to context.getString(R.string.strDescVerseNoWithChapter, name, verseNo)
        } else {
            context.getString(
                R.string.strLabelVerseSerial,
                chapterNo,
                verseNo
            ) to context.getString(R.string.strDescVerseNo, verseNo)
        }
    }

    Text(
        text = label,
        modifier = Modifier
            .semantics { contentDescription = serialContentDescription }
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
