package com.quranapp.android.compose.components.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.quranapp.android.R
import com.quranapp.android.compose.theme.alpha

private const val LIBRARY_PREVIEW_LIST_COUNT = 3
private const val LIBRARY_PREVIEW_GRID_COUNT = 4
private const val LIBRARY_PREVIEW_GRID_COLUMNS = 2

@Composable
fun <T> LibraryPreviewList(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
) {
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    val visibleItems = items.take(
        if (wide) LIBRARY_PREVIEW_GRID_COUNT else LIBRARY_PREVIEW_LIST_COUNT,
    )
    if (visibleItems.isEmpty()) return

    if (wide) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            visibleItems.chunked(LIBRARY_PREVIEW_GRID_COLUMNS).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { item ->
                        LibraryPreviewSurface(modifier = Modifier.weight(1f)) {
                            itemContent(item)
                        }
                    }
                    if (rowItems.size < LIBRARY_PREVIEW_GRID_COLUMNS) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        LibraryPreviewSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column {
                visibleItems.forEachIndexed { index, item ->
                    itemContent(item)
                    if (index < visibleItems.lastIndex) {
                        HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryPreviewSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(shapes.medium)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.3f), shapes.medium),
        color = colorScheme.surfaceContainer.alpha(0.5f),
        content = content,
    )
}


@Composable
fun SectionTitle(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    headerRightContent: (@Composable () -> Unit)?,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            tint = colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )

        headerRightContent?.let {
            it()
        }
    }
}


@Composable
fun SectionEmptyMessage(
    message: String,
    horizontalPadding: Dp = 16.dp,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), shapes.medium)
            .padding(20.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionHeaderActionButton(
    text: String,
    icon: Int? = null,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = colorScheme.onBackground.alpha(0.85f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outline.alpha(0.25f),
        ),
        shape = shapes.small,
        modifier = Modifier.height(28.dp),
        contentPadding = PaddingValues(
            start = if (icon != null) 6.dp else 10.dp,
            end = 10.dp,
            top = 0.dp,
            bottom = 0.dp,
        ),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun SectionHeaderViewAll(
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = colorScheme.onBackground.alpha(0.85f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outline.alpha(0.25f),
        ),
        shape = shapes.small,
        modifier = Modifier.height(28.dp),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 5.dp,
            top = 0.dp,
            bottom = 0.dp,
        ),
    ) {
        Text(
            text = stringResource(R.string.strLabelViewAll),
            style = MaterialTheme.typography.labelMedium,
        )

        Icon(
            painter = painterResource(R.drawable.dr_icon_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}
