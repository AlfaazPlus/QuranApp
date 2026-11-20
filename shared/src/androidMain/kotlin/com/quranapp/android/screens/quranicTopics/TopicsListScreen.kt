package com.quranapp.android.screens.quranicTopics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.IconButton
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.quranicTopics.EmptyContent
import com.quranapp.android.components.quranicTopics.ListIntroCard
import com.quranapp.android.components.quranicTopics.TopicListItem
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.about_this_page
import com.quranapp.android.generated.resources.dr_icon_info
import com.quranapp.android.generated.resources.dr_icon_search
import com.quranapp.android.generated.resources.englishContentOnly
import com.quranapp.android.generated.resources.noItems
import com.quranapp.android.generated.resources.strLabelGotIt
import com.quranapp.android.viewmodel.TopicNode
import com.quranapp.android.viewmodel.TopicsTree
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun QuranicTopicListRoute(
    title: String,
    tree: TopicsTree,
    isLoading: Boolean,
    topics: List<TopicNode>,
    primaryTopicCount: Int,
    hasMoreSupplementalPages: Boolean,
    isLoadingMoreSupplemental: Boolean,
    onLoadMoreSupplemental: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    var infoDialogShown by remember { mutableStateOf(false) }
    val navController = LocalTopicsNavController.current
    val listState = rememberLazyListState()
    val clampedPrimaryCount = primaryTopicCount.coerceIn(0, topics.size)

    val primaryTopics = remember(topics, clampedPrimaryCount) {
        topics.take(clampedPrimaryCount)
    }

    val supplementalTopics = remember(topics, clampedPrimaryCount) {
        topics.drop(clampedPrimaryCount)
    }

    LaunchedEffect(listState, topics.size, hasMoreSupplementalPages, isLoadingMoreSupplemental) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount

            if (total <= 0) return@snapshotFlow false

            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= total - 4
        }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd && hasMoreSupplementalPages && !isLoadingMoreSupplemental }
            .collect {
                onLoadMoreSupplemental()
            }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            AppBar(
                title = title,
                actions = {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_search)
                    ) {
                        onOpenSearch()
                    }
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_info)
                    ) {
                        infoDialogShown = true
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading -> Loader(true)
            topics.isEmpty() -> EmptyContent(
                modifier = Modifier.padding(padding),
                message = stringResource(Res.string.noItems),
            )

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = 64.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "intro") {
                        ListIntroCard(tree = tree)
                    }

                    items(primaryTopics, key = { it.id }) { topic ->
                        TopicListItem(
                            topic = topic,
                            accent = if (tree == TopicsTree.Ontology) {
                                colorScheme.primary
                            } else {
                                colorScheme.tertiary
                            },
                            onClick = {
                                navController.navigate(
                                    QuranicTopicRoutes.topic(
                                        tree = tree,
                                        topicId = topic.id,
                                    ),
                                    navOptions = topicsNavOptions()
                                )
                            }
                        )
                    }

                    if (supplementalTopics.isNotEmpty()) {
                        item(key = "supplemental_header") {
                            Text(
                                text = "More topics from broader catalog",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                            )
                        }
                        items(supplementalTopics, key = { "supplemental_${it.id}" }) { topic ->
                            TopicListItem(
                                topic = topic,
                                accent = if (tree == TopicsTree.Ontology) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.tertiary
                                },
                                onClick = {
                                    navController.navigate(
                                        QuranicTopicRoutes.topic(
                                            tree = tree,
                                            topicId = topic.id,
                                        ),
                                        navOptions = topicsNavOptions()
                                    )
                                },
                            )
                        }
                    }

                    if (hasMoreSupplementalPages || isLoadingMoreSupplemental) {
                        item(key = "load_more_footer") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (isLoadingMoreSupplemental) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text(
                                        text = "Loading more topics…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        text = "Scroll down to load more topics.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        isOpen = infoDialogShown,
        onClose = { infoDialogShown = false },
        title = stringResource(Res.string.about_this_page),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelGotIt),
            ),
        ),
    ) {
        Text(
            stringResource(Res.string.englishContentOnly)
        )
    }
}
