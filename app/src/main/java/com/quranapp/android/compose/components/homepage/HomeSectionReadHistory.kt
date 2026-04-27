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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfaazplus.sunnah.ui.theme.tightTextStyle
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReadHistory
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.screens.subtitleLabel
import com.quranapp.android.compose.screens.titleLabel
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.ReadHistoryEntity
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReadHistoryViewModel

@Composable
fun HomeSectionReadHistory() {
    val viewModel = viewModel<ReadHistoryViewModel>()
    val recentHistories by viewModel.recentHistories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = R.drawable.dr_icon_history,
            title = R.string.strTitleReadHistory,
            onViewAllClick = {
                context.startActivity(Intent(context, ActivityReadHistory::class.java))
            },
        )

        when {
            recentHistories.isEmpty() -> {
                Text(
                    text = stringResource(R.string.strMsgReadShowupHere),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }

            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(recentHistories, key = { it.id }) { history ->
                        ItemCard(
                            history = history,
                            chapterName = history.chapterName.orEmpty(),
                            onOpen = {
                                ReaderFactory.prepareHistoryIntent(history)?.let {
                                    it.setClass(context, ActivityReader::class.java)
                                    context.startActivity(it)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ItemCard(
    history: ReadHistoryEntity,
    chapterName: String,
    onOpen: () -> Unit,
) {
    val readType = ReadType.fromValue(history.readType)
    val title = history.titleLabel(chapterName)
    val subtitle = history.subtitleLabel(chapterName)

    val accentColor = when (readType) {
        ReadType.Chapter -> colorScheme.primary
        ReadType.Juz -> colorScheme.tertiary
        ReadType.Hizb -> colorScheme.secondary
    }

    Surface(
        modifier = Modifier
            .height(100.dp)
            .width(280.dp)
            .shadow(1.dp, shapes.medium)
            .clip(shapes.medium)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.7f), shapes.medium)
            .clickable(onClick = onOpen),
        color = colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                when (ReaderMode.fromValue(history.readerMode)) {
                                    ReaderMode.Reading -> R.drawable.ic_mode_mushaf
                                    ReaderMode.Translation -> R.drawable.ic_mode_translation
                                    else -> R.drawable.ic_mode_verse
                                }
                            ),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = typography.titleSmall.merge(tightTextStyle),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface,
                    )

                    subtitle?.let {
                        Text(
                            text = it,
                            style = typography.bodyMedium.merge(tightTextStyle),
                            color = colorScheme.onSurface.alpha(0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = stringResource(R.string.strLabelContinueReading),
                            style = typography.labelMedium.merge(tightTextStyle),
                            color = colorScheme.onSurface.alpha(0.65f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}