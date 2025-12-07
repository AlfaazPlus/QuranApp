package com.quranapp.android.composables.verse

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Verse

sealed class VerseActionType {
    object Options : VerseActionType()
    object Recite : VerseActionType()
    object Tafsir : VerseActionType()
    object BookmarkToggle : VerseActionType()
}


@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)
) {
    Box(
        modifier = modifier
            .padding(end = 8.dp)
            .size(dimensionResource(R.dimen.dmnActionButton))
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun RowScope.VerseActionBar(
    verse: Verse,
    isBookmarked: Boolean,
    isReciting: Boolean,
    onActionSelected: (VerseActionType) -> Unit
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(
            onClick = {
                onActionSelected(VerseActionType.Options)
            },
        ) {
            Image(
                painter = painterResource(id = R.drawable.dr_icon_menu),
                contentDescription = stringResource(R.string.strTitleVerseOptions),
                colorFilter = ColorFilter.tint(colorResource(R.color.colorIcon2)),
                modifier = Modifier.rotate(90f)
            )
        }

        QuickActionButton(
            onClick = {
                onActionSelected(VerseActionType.Recite)
            }
        ) {
            Image(
                painter = painterResource(
                    id = if (isReciting) R.drawable.dr_icon_pause_verse else R.drawable.dr_icon_play_verse
                ),
                contentDescription = stringResource(R.string.strTitleVerseRecitation),
                colorFilter = ColorFilter.tint(colorResource(R.color.colorIcon2)),
            )
        }

        QuickActionButton(
            onClick = {
                onActionSelected(VerseActionType.Tafsir)
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.dr_icon_tafsir),
                contentDescription = stringResource(R.string.strTitleTafsir),
            )
        }

        QuickActionButton(
            onClick = {
                onActionSelected(VerseActionType.BookmarkToggle)
            }
        ) {
            Image(
                painter = painterResource(
                    id = if (isBookmarked) R.drawable.dr_icon_bookmark_added else R.drawable.dr_icon_bookmark_outlined
                ),
                contentDescription = stringResource(R.string.strLabelBookmark),
                colorFilter = ColorFilter.tint(
                    if (isBookmarked) colorResource(R.color.colorPrimary) else colorResource(R.color.colorIcon2)
                ),
            )
        }
    }
}