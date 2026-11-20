package com.quranapp.android.components.reader.dialogs

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.common.dialogs.BottomSheetHeader
import com.quranapp.android.components.reader.LocalReaderViewModel
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.add_to_collection
import com.quranapp.android.generated.resources.dr_icon_footnote
import com.quranapp.android.generated.resources.dr_icon_report_problem
import com.quranapp.android.generated.resources.dr_icon_share
import com.quranapp.android.generated.resources.ic_book_copy
import com.quranapp.android.generated.resources.ic_image
import com.quranapp.android.generated.resources.ic_library
import com.quranapp.android.generated.resources.noFootnotesForThisVerse
import com.quranapp.android.generated.resources.noSimilarVerses
import com.quranapp.android.generated.resources.shareAsImage
import com.quranapp.android.generated.resources.similarVerses
import com.quranapp.android.generated.resources.strLabelReport
import com.quranapp.android.generated.resources.strLabelShare
import com.quranapp.android.generated.resources.strTitleFootnotes
import com.quranapp.android.generated.resources.strTitleReaderVerseInformation
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.miscellenous.Keys
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseOptionsSheet(
    vwd: VerseWithDetails?,
    onFootnotes: (VerseWithDetails) -> Unit,
    onAddToCollection: (VerseWithDetails) -> Unit,
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
            onAddToCollection = {
                onAddToCollection(vwd)
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
    onAddToCollection: () -> Unit,
) {
    val repository = LocalReaderViewModel.current.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val title = stringResource(
        Res.string.strTitleReaderVerseInformation,
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
                iconRes = Res.drawable.dr_icon_footnote,
                labelRes = Res.string.strTitleFootnotes,
                enabled = hasFootnotes,
                onClick = {
                    if (hasFootnotes) {
                        onDismiss()
                        onFootnotes(verse)
                    } else {
                        scope.launch {
                            Platform.messages.show(
                                getString(Res.string.noFootnotesForThisVerse),
                                long = true
                            )
                        }
                    }
                },
            )

            VodOptionItem(
                iconRes = Res.drawable.ic_book_copy,
                labelRes = Res.string.similarVerses,
                enabled = hasSimilarVerses,
                onClick = {
                    if (hasSimilarVerses) {
                        onDismiss()
                        onSimilarVerses(verse)
                    } else {
                        scope.launch {
                            Platform.messages.show(
                                getString(Res.string.noSimilarVerses),
                                long = true
                            )
                        }
                    }
                }
            )

            VodOptionItem(
                iconRes = Res.drawable.ic_image,
                labelRes = Res.string.shareAsImage,
                onClick = {
                    onDismiss()
                    Platform.navigator.navigate(
                        AppRoute.Screen(
                            screen = AppRoutes.SHARE_AS_IMAGE,
                            extras = mapOf(
                                Keys.READER_KEY_CHAPTER_NO to verse.chapterNo,
                                Keys.READER_KEY_VERSE_NO to verse.verseNo,
                            )
                        )
                    )
                },
            )

            VodOptionItem(
                iconRes = Res.drawable.dr_icon_share,
                labelRes = Res.string.strLabelShare,
                onClick = onShare,
            )

            VodOptionItem(
                iconRes = Res.drawable.ic_library,
                labelRes = Res.string.add_to_collection,
                onClick = onAddToCollection,
            )

            VodOptionItem(
                iconRes = Res.drawable.dr_icon_report_problem,
                labelRes = Res.string.strLabelReport,
                onClick = {
                    onDismiss()
                    Platform.links.browse(ApiConfig.GITHUB_ISSUES_VERSE_REPORT_URL)
                },
            )
        }
    }
}

@Composable
private fun VodOptionItem(
    iconRes: DrawableResource,
    labelRes: StringResource,
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
