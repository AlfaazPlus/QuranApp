package com.quranapp.android.compose.screens.quranictopics.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.viewModels.QuranicTopicNode
import horizontalFadingEdge


@Composable
fun BreadcrumbTrail(
    rootLabel: String,
    breadcrumbs: List<QuranicTopicNode>,
    currentTopic: QuranicTopicNode,
    onRootClick: () -> Unit,
    onBreadcrumbClick: (QuranicTopicNode, Int) -> Unit,
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(currentTopic) {
        scrollState.scrollToItem(breadcrumbs.size)
    }

    Box(
        Modifier.horizontalFadingEdge(scrollState, color = colorScheme.surfaceContainer)
    ) {
        LazyRow(
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                BreadcrumbText(
                    text = rootLabel,
                    isCurrent = false,
                    onClick = onRootClick,
                )
            }

            breadcrumbs.forEachIndexed { index, breadcrumb ->
                item { BreadcrumbSeparator() }
                item {
                    BreadcrumbText(
                        text = breadcrumb.title,
                        isCurrent = false,
                        onClick = { onBreadcrumbClick(breadcrumb, index) },
                    )
                }
            }

            item { BreadcrumbSeparator() }

            item {
                BreadcrumbText(
                    text = currentTopic.title,
                    isCurrent = true,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbSeparator() {
    Text(
        text = "›",
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.onSurface.alpha(0.65f),
    )
}

@Composable
private fun BreadcrumbText(
    text: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (isCurrent) colorScheme.onSurface else colorScheme.onSurface.alpha(0.65f),
        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .then(if (isCurrent) Modifier else Modifier.clickable(onClick = onClick)),
    )
}
