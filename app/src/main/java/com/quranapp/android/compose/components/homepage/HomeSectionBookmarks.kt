package com.quranapp.android.compose.components.homepage

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityBookmark
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerData
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.viewModels.BookmarksViewModel

private const val HOME_BOOKMARKS_LIMIT = 10

@Composable
fun HomeSectionBookmarks() {
    val viewModel = viewModel<BookmarksViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chapterNames by viewModel.chapterNames.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var viewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    val bookmarks = uiState.bookmarks.take(HOME_BOOKMARKS_LIMIT)

    BookmarkViewerSheet(data = viewerData, onClose = { viewerData = null })

    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = R.drawable.ic_bookmark,
            title = R.string.strTitleBookmarks,
            onViewAllClick = {
                context.startActivity(Intent(context, ActivityBookmark::class.java))
            },
        )

        when {
            !uiState.isLoading && bookmarks.isEmpty() -> {
                Text(
                    text = stringResource(R.string.strMsgBookmarkNoItems),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }

            bookmarks.isNotEmpty() -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        BookmarkCard(
                            bookmark = bookmark,
                            chapterName = chapterNames[bookmark.chapterNo].orEmpty(),
                            onClick = {
                                viewerData = BookmarkViewerData(
                                    chapterNo = bookmark.chapterNo,
                                    fromVerse = bookmark.fromVerseNo,
                                    toVerse = bookmark.toVerseNo,
                                )
                            },
                        )
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

    Surface(
        modifier = Modifier
            .height(100.dp)
            .width(280.dp)
            .clip(shapes.medium)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.5f), shapes.medium)
            .clickable(onClick = onClick),
        color = colorScheme.surfaceContainer.alpha(0.75f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Top,
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


            bookmark.note?.takeIf { it.isNotBlank() }?.let { note ->
                Icon(
                    painter = painterResource(R.drawable.dr_icon_edit),
                    contentDescription = null,
                    tint = colorScheme.primary.alpha(0.75f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
