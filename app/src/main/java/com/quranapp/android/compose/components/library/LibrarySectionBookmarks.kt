package com.quranapp.android.compose.components.library

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityBookmark
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerData
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.viewModels.BookmarksViewModel

@Composable
fun LibrarySectionBookmarks() {
    val viewModel = viewModel<BookmarksViewModel>()
    val recentBookmarks by viewModel.recentBookmarks.collectAsStateWithLifecycle()
    val chapterNames by viewModel.chapterNames.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var viewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }

    BookmarkViewerSheet(data = viewerData, onClose = { viewerData = null })

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(
            iconRes = R.drawable.ic_bookmark,
            title = stringResource(R.string.strTitleBookmarks),
            headerRightContent = if (recentBookmarks.isNotEmpty()) {
                {
                    SectionHeaderViewAll {
                        context.startActivity(Intent(context, ActivityBookmark::class.java))
                    }
                }
            } else null,
        )

        when {
            recentBookmarks.isEmpty() -> {
                SectionEmptyMessage(
                    message = stringResource(R.string.strMsgBookmarkNoItems),
                )
            }

            else -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(shapes.medium)
                        .border(1.dp, colorScheme.outlineVariant.alpha(0.3f), shapes.medium),
                    color = colorScheme.surfaceContainer.alpha(0.5f),
                ) {
                    Column() {
                        recentBookmarks.forEachIndexed { index, bookmark ->
                            BookmarkCard(
                                bookmark = bookmark,
                                chapterName = chapterNames.get(bookmark.chapterNo).orEmpty(),
                                onClick = {
                                    viewerData = BookmarkViewerData(
                                        chapterNo = bookmark.chapterNo,
                                        fromVerse = bookmark.fromVerseNo,
                                        toVerse = bookmark.toVerseNo,
                                    )
                                },
                            )

                            if (index < recentBookmarks.lastIndex) {
                                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: BookmarkEntity,
    chapterName: String,
    onClick: () -> Unit,
) {
    val verseLabel = if (bookmark.fromVerseNo == bookmark.toVerseNo) {
        stringResource(R.string.strLabelVerseNo, bookmark.fromVerseNo)
    } else {
        stringResource(R.string.strLabelVerses, bookmark.fromVerseNo, bookmark.toVerseNo)
    }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = bookmark.chapterNo.toString(),
                        style = typography.bodyLarge,
                        fontWeight = FontWeight.Light,
                        color = colorScheme.onSurface,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapterName,
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurface,
                )

                Text(
                    text = verseLabel,
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.onSurfaceVariant.alpha(0.65f)
            )
        }


        bookmark.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = buildAnnotatedString {
                    appendInlineContent("user_note", "[icon]")
                    append(" ")
                    append(bookmark.note.take(50))
                },
                inlineContent = mapOf(
                    "user_note" to InlineTextContent(
                        Placeholder(
                            width = 16.sp,
                            height = 16.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_edit),
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 58.dp, top = 8.dp),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}
