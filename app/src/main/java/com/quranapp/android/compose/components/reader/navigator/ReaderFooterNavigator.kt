package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.ReaderIntentData
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import kotlinx.coroutines.launch


@Composable
fun ReaderFooterNavigator(
    readerVm: ReaderViewModel,
    viewType: ReaderViewType,
    listState: LazyListState,
) {
    val scope = rememberCoroutineScope()
    val prevEnabled = remember(viewType) {
        adjacentDivisionLaunchParams(viewType, -1) != null
    }
    val nextEnabled = remember(viewType) {
        adjacentDivisionLaunchParams(viewType, 1) != null
    }

    val previousChapterSubtitle by produceState<String?>(
        initialValue = null,
        viewType,
        prevEnabled,
        readerVm,
    ) {
        if (!prevEnabled || viewType !is ReaderViewType.Chapter) {
            value = null
            return@produceState
        }

        val n = viewType.chapterNo - 1

        value = if (n in QuranMeta.chapterRange) {
            readerVm.repository.getChapterName(n)
        } else {
            null
        }
    }

    val nextChapterSubtitle by produceState<String?>(
        initialValue = null,
        viewType,
        nextEnabled,
        readerVm,
    ) {
        if (!nextEnabled || viewType !is ReaderViewType.Chapter) {
            value = null
            return@produceState
        }
        val n = viewType.chapterNo + 1
        value = if (n in QuranMeta.chapterRange) {
            readerVm.repository.getChapterName(n)
        } else {
            null
        }
    }

    val previousSubtitle: String? = when {
        !prevEnabled -> null
        viewType is ReaderViewType.Juz -> stringResource(R.string.strLabelJuzNo, viewType.juzNo - 1)

        viewType is ReaderViewType.Hizb -> stringResource(R.string.labelHizbNo, viewType.hizbNo - 1)

        viewType is ReaderViewType.Chapter -> previousChapterSubtitle
        else -> null
    }

    val nextSubtitle: String? = when {
        !nextEnabled -> null
        viewType is ReaderViewType.Juz -> stringResource(R.string.strLabelJuzNo, viewType.juzNo + 1)

        viewType is ReaderViewType.Hizb -> stringResource(R.string.labelHizbNo, viewType.hizbNo + 1)

        viewType is ReaderViewType.Chapter -> nextChapterSubtitle
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationButton(
            enabled = prevEnabled,
            label = when (viewType) {
                is ReaderViewType.Chapter -> R.string.previousChapter
                is ReaderViewType.Juz -> R.string.previousJuz
                is ReaderViewType.Hizb -> R.string.previousHizb
            },
            subtitle = previousSubtitle,
            isNext = false,
        ) {
            listState.requestScrollToItem(0)

            scope.launch {
                adjacentDivisionLaunchParams(viewType, -1)?.let { readerVm.initReader(it) }
            }
        }

        Box(
            modifier = Modifier
                .height(56.dp)
                .width(48.dp)
                .clip(shapes.medium)
                .background(colorScheme.surfaceContainer)
                .clickable {
                    listState.requestScrollToItem(0)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_arrow_up),
                contentDescription = stringResource(R.string.strLabelTop),
                tint = colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        NavigationButton(
            enabled = nextEnabled,
            label = when (viewType) {
                is ReaderViewType.Chapter -> R.string.nextChapter
                is ReaderViewType.Juz -> R.string.nextJuz
                is ReaderViewType.Hizb -> R.string.nextHizb
            },
            subtitle = nextSubtitle,
            isNext = true,
        ) {
            scope.launch {
                adjacentDivisionLaunchParams(viewType, 1)?.let { readerVm.initReader(it) }
            }
        }
    }
}

@Composable
private fun RowScope.NavigationButton(
    enabled: Boolean,
    label: Int,
    subtitle: String?,
    isNext: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (enabled) {
        colorScheme.surfaceContainer
    } else {
        colorScheme.surfaceContainer.copy(0.5f)
    }

    val contentColor = if (enabled) {
        colorScheme.onSurface.copy(alpha = 0.8f)
    } else {
        colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(
                shapes.medium
            )
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, if (isNext) Alignment.End else Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isNext) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_left),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            horizontalAlignment = if (isNext) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = typography.bodySmall,
                    color = colorScheme.onSurface.copy(0.5f),
                    maxLines = 1,
                    modifier = Modifier
                        .basicMarquee(
                            initialDelayMillis = 900,
                            repeatDelayMillis = 1_200,
                        ),
                )
            }
        }

        if (isNext) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

internal fun adjacentDivisionLaunchParams(
    viewType: ReaderViewType,
    direction: Int,
): ReaderLaunchParams? {
    require(direction == -1 || direction == 1)

    return when (viewType) {
        is ReaderViewType.Chapter -> {
            val n = viewType.chapterNo + direction
            if (n in QuranMeta.chapterRange) {
                ReaderLaunchParams(ReaderIntentData.FullChapter(n))
            } else {
                null
            }
        }

        is ReaderViewType.Juz -> {
            val n = viewType.juzNo + direction
            if (n in QuranMeta.juzRange) {
                ReaderLaunchParams(ReaderIntentData.FullJuz(n))
            } else {
                null
            }
        }

        is ReaderViewType.Hizb -> {
            val n = viewType.hizbNo + direction
            if (n in QuranMeta.hizbRange) {
                ReaderLaunchParams(ReaderIntentData.FullHizb(n))
            } else {
                null
            }
        }
    }
}