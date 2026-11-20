package com.quranapp.android.components.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.continue_reading
import com.quranapp.android.generated.resources.dr_icon_chevron_right
import com.quranapp.android.generated.resources.dr_icon_history
import com.quranapp.android.generated.resources.strMsgReadShowupHere
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.reader.ReaderMode
import com.quranapp.android.screens.accentColor
import com.quranapp.android.screens.iconRes
import com.quranapp.android.screens.subtitleLabel
import com.quranapp.android.screens.titleLabel
import com.quranapp.android.theme.alpha
import com.quranapp.android.theme.tightTextStyle
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewmodel.ReadHistoryViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibrarySectionReadHistory() {
    val viewModel = appViewModel<ReadHistoryViewModel>()
    val chapterNames by viewModel.chapterNames.collectAsStateWithLifecycle()
    val recentHistories by viewModel.recentHistories.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(
            iconRes = Res.drawable.dr_icon_history,
            title = stringResource(Res.string.continue_reading),
            headerRightContent = if (recentHistories.isNotEmpty()) {
                {
                    SectionHeaderViewAll {
                        Platform.navigator.navigate(AppRoute.Screen(AppRoutes.READ_HISTORY))
                    }
                }
            } else null,
        )

        when {
            recentHistories.isEmpty() -> {
                SectionEmptyMessage(
                    message = stringResource(Res.string.strMsgReadShowupHere),
                )
            }

            else -> {
                LibraryPreviewList(items = recentHistories) { history ->
                    ItemCard(
                        history = history,
                        chapterName = chapterNames[history.chapterNo].orEmpty(),
                        onOpen = {
                            ReaderFactory.fromHistory(history)?.let {
                                Platform.navigator.navigate(AppRoute.Reader(it))
                            }
                        },
                    )
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
    if (history.isMain) {
        ActiveItemCard(
            history = history,
            chapterName = chapterName,
            onOpen = onOpen,
        )
    } else {
        RegularItemCard(
            history = history,
            chapterName = chapterName,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun ActiveItemCard(
    history: ReadHistoryEntity,
    chapterName: String,
    onOpen: () -> Unit,
) {
    val readerMode = ReaderMode.fromValue(history.readerMode)
    val title = history.titleLabel(chapterName)
    val subtitle = history.subtitleLabel(chapterName)

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onOpen,
        color = colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = shapes.medium,
                color = colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(readerMode.iconRes()),
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = typography.labelLarge.merge(tightTextStyle),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium.merge(tightTextStyle),
                        color = colorScheme.onSurface.alpha(0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegularItemCard(
    history: ReadHistoryEntity,
    chapterName: String,
    onOpen: () -> Unit,
) {
    val readerMode = ReaderMode.fromValue(history.readerMode)
    val title = history.titleLabel(chapterName)
    val subtitle = history.subtitleLabel(chapterName)
    val accentColor = readerMode.accentColor()

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
                shape = shapes.medium,
                color = colorScheme.surfaceVariant.alpha(0.55f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(readerMode.iconRes()),
                        contentDescription = null,
                        tint = accentColor,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium.merge(tightTextStyle),
                        color = colorScheme.onSurface.alpha(0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.onSurfaceVariant.alpha(0.65f)
            )
        }
    }
}
