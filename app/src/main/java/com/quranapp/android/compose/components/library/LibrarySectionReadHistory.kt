package com.quranapp.android.compose.components.library

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReadHistory
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.screens.subtitleLabel
import com.quranapp.android.compose.screens.titleLabel
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.theme.tightTextStyle
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReadHistoryViewModel

@Composable
fun LibrarySectionReadHistory() {
    val viewModel = viewModel<ReadHistoryViewModel>()
    val chapterNames by viewModel.chapterNames.collectAsStateWithLifecycle()
    val recentHistories by viewModel.recentHistories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(
            iconRes = R.drawable.dr_icon_history,
            title = stringResource(R.string.continue_reading),
            headerRightContent = if (recentHistories.isNotEmpty()) {
                {
                    SectionHeaderViewAll {
                        context.startActivity(Intent(context, ActivityReadHistory::class.java))
                    }
                }
            } else null,
        )

        when {
            recentHistories.isEmpty() -> {
                SectionEmptyMessage(
                    message = stringResource(R.string.strMsgReadShowupHere),
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
                        recentHistories.forEachIndexed { index, history ->
                            ItemCard(
                                history = history,
                                chapterName = chapterNames.get(history.chapterNo).orEmpty(),
                                onOpen = {
                                    ReaderFactory.prepareHistoryIntent(history)?.let {
                                        it.setClass(context, ActivityReader::class.java)
                                        context.startActivity(it)
                                    }
                                },
                            )

                            if (index < recentHistories.lastIndex) {
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

    Column(
        modifier = Modifier
            .clickable(onClick = onOpen)
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
                color = accentColor.copy(alpha = 0.35f),
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
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = typography.labelLarge.merge(tightTextStyle),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium.merge(tightTextStyle),
                        color = colorScheme.onSurface.alpha(0.75f),
                    )
                }
            }


            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.onSurfaceVariant.alpha(0.65f)
            )
        }
    }
}
