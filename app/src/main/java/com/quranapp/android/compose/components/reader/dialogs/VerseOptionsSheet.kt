package com.quranapp.android.compose.components.reader.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.compose.components.dialogs.BottomSheetHeader
import com.quranapp.android.compose.components.reader.LocalReaderViewModel
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.relations.VerseWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseOptionsSheet(
    vwd: VerseWithDetails?,
    onFootnotes: (VerseWithDetails) -> Unit,
    onClose: () -> Unit,
) {
    var shareSheetData by remember { mutableStateOf<VerseWithDetails?>(null) }
    var similarVersesSheetData by remember { mutableStateOf<VerseWithDetails?>(null) }

    VerseShareSheet(
        vwd = shareSheetData,
        onDismiss = { shareSheetData = null },
    )

    SimilarVersesSheet(
        sourceVerse = similarVersesSheetData,
        onDismiss = { similarVersesSheetData = null },
    )

    val sheetState = rememberModalBottomSheetState(true)

    if (vwd == null) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        VodSheetContent(
            verse = vwd,
            onDismiss = onClose,
            onFootnotes = onFootnotes,
            onSimilarVerses = {
                similarVersesSheetData = it
            },
            onShare = {
                shareSheetData = vwd
                onClose()
            },
        )
    }
}

@Composable
private fun VodSheetContent(
    verse: VerseWithDetails,
    onDismiss: () -> Unit,
    onFootnotes: (VerseWithDetails) -> Unit,
    onSimilarVerses: (VerseWithDetails) -> Unit,
    onShare: () -> Unit,
) {
    val repository = LocalReaderViewModel.current.repository
    val context = LocalContext.current

    val title = stringResource(
        R.string.strTitleReaderVerseInformation,
        verse.chapter.getCurrentName(),
        verse.verseNo
    )

    val hasFootnotes = remember(verse) {
        verse.translations.any { it.getFootnotesCount() > 0 }
    }

    val hasSimilarVerses by produceState(false, verse.id) {
        value = repository.extrasDao.countSimilarVerses(verse.id) > 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        BottomSheetHeader(
            title = title,
            hasDragHandle = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VodOptionItem(
                iconRes = R.drawable.dr_icon_footnote,
                labelRes = R.string.strTitleFootnotes,
                enabled = hasFootnotes,
                onClick = {
                    if (hasFootnotes) {
                        onDismiss()
                        onFootnotes(verse)
                    } else {
                        Toast.makeText(
                            context,
                            R.string.noFootnotesForThisVerse,
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                },
            )

            VodOptionItem(
                iconRes = R.drawable.ic_book_copy,
                labelRes = R.string.similarVerses,
                enabled = hasSimilarVerses,
                onClick = {
                    if (hasSimilarVerses) {
                        onDismiss()
                        onSimilarVerses(verse)
                    } else {
                        Toast.makeText(
                            context,
                            R.string.noSimilarVerses,
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            )

            VodOptionItem(
                iconRes = R.drawable.dr_icon_share,
                labelRes = R.string.strLabelShare,
                onClick = onShare,
            )

            VodOptionItem(
                iconRes = R.drawable.dr_icon_report_problem,
                labelRes = R.string.strLabelReport,
                onClick = {
                    onDismiss()
                    AppBridge.newOpener(context)
                        .browseLink(ApiConfig.GITHUB_ISSUES_VERSE_REPORT_URL)
                },
            )
        }
    }
}

@Composable
private fun VodOptionItem(
    iconRes: Int,
    labelRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.5f
    val tint = colorScheme.onBackground.alpha(0.7f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(alpha)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(26.dp),
                tint = tint,
            )
        }
        Text(
            text = stringResource(labelRes),
            style = typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
