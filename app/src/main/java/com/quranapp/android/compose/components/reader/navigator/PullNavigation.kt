package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.quranapp.android.R
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import kotlinx.coroutines.launch

@Composable
fun PullNavigation(
    readerVm: ReaderViewModel,
    viewType: ReaderViewType?,
    content: @Composable () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val shiftPx = with(density) {
        (pullRefreshState.distanceFraction.fastCoerceIn(
            0f,
            1f
        ) * (PULL_INDICATOR_SHIFT_DISTANCE * 2.5).dp.toPx()).toInt()
    }


    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = false,
        onRefresh = {
            viewType?.let { vt ->
                scope.launch {
                    adjacentDivisionLaunchParams(vt, -1)?.let { readerVm.initReader(it) }
                }
            }
        },
        indicator = { Indicator(pullRefreshState, viewType, readerVm) }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = 0,
                        y = shiftPx
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun BoxScope.Indicator(
    state: PullToRefreshState,
    viewType: ReaderViewType?,
    readerVm: ReaderViewModel,
) {
    val vt = viewType ?: return

    val labelRes = when (vt) {
        is ReaderViewType.Chapter -> R.string.previousChapter
        is ReaderViewType.Juz -> R.string.previousJuz
        is ReaderViewType.Hizb -> R.string.previousHizb
    }

    val prevEnabled = remember(vt) {
        adjacentDivisionLaunchParams(vt, -1) != null
    }

    val previousChapterSubtitle by produceState<String?>(
        initialValue = null,
        vt,
        prevEnabled,
        readerVm,
    ) {
        if (!prevEnabled || vt !is ReaderViewType.Chapter) {
            value = null
            return@produceState
        }
        val n = vt.chapterNo - 1
        value = if (n in QuranMeta.chapterRange) {
            readerVm.repository.getChapterName(n)
        } else {
            null
        }
    }

    val previousSubtitle: String? = when {
        !prevEnabled -> null
        vt is ReaderViewType.Juz -> stringResource(R.string.strLabelJuzNo, vt.juzNo - 1)
        vt is ReaderViewType.Hizb -> stringResource(R.string.labelHizbNo, vt.hizbNo - 1)
        vt is ReaderViewType.Chapter -> previousChapterSubtitle
        else -> null
    }

    val fraction = state.distanceFraction.fastCoerceIn(0f, 1f)
    if (fraction <= 0f) return

    val density = LocalDensity.current
    val shiftPx = with(density) {
        (fraction * PULL_INDICATOR_SHIFT_DISTANCE.dp.toPx()).toInt()
    }

    Surface(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, shiftPx) }
            .alpha(fraction),
        shape = shapes.medium,
        color = colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(labelRes),
                style = typography.labelMedium,
                color = colorScheme.onSurface.copy(alpha = 0.8f),
            )

            Text(
                text = previousSubtitle ?: stringResource(R.string.noItems),
                style = typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                modifier = Modifier.basicMarquee(
                    initialDelayMillis = 900,
                    repeatDelayMillis = 1_200,
                ),
            )
        }
    }
}

const val PULL_INDICATOR_SHIFT_DISTANCE = 36