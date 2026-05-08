package com.quranapp.android.compose.screens.quranictopics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.quranic_topics.EmptyContent
import com.quranapp.android.compose.components.quranic_topics.RelationshipItem
import com.quranapp.android.compose.components.quranic_topics.SectionHeader
import com.quranapp.android.compose.components.quranic_topics.TopicExploreCard
import com.quranapp.android.compose.components.quranic_topics.TopicHeroCard
import com.quranapp.android.compose.components.quranic_topics.TopicListItem
import com.quranapp.android.compose.components.quranic_topics.VerseRefsCard
import com.quranapp.android.viewModels.TopicDetailUiState
import com.quranapp.android.viewModels.TopicNode
import com.quranapp.android.viewModels.TopicsTree

@Composable
internal fun QuranicTopicDetailRoute(
    state: TopicDetailUiState,
    roots: List<TopicNode>,
    tree: TopicsTree,
    topicId: Int,
) {
    val navController = LocalTopicsNavController.current
    val topic = state.topic
    val currentTrail = state.breadcrumbs.map { it.id }
    val listState = rememberSaveable(topicId, saver = LazyListState.Saver) {
        LazyListState()
    }

    val showTitleInTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 500
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBar(
                title = if (showTitleInTopBar) topic?.title ?: "Topic" else "",
                shadowElevation = if (showTitleInTopBar) 4.dp else 0.dp
            )
        },
        containerColor = colorScheme.background,
    ) { padding ->
        when {
            state.isLoading && topic == null -> Loader(true)
            topic == null -> EmptyContent(
                message = "Topic not found"
            )

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(
                        bottom = 128.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        TopicHeroCard(
                            navController = navController,
                            topic = topic,
                            tree = tree,
                            breadcrumbs = state.breadcrumbs,
                            visibleRelatedCount = state.relationships.size,
                        )
                    }

                    item {
                        TopicExploreCard(
                            topic = topic,
                            hasVerses = state.verseRefs.isNotEmpty(),
                            hasSubtopics = state.childTopics.isNotEmpty() || state.broaderCatalogChildren.isNotEmpty(),
                            hasRelated = state.relationships.isNotEmpty(),
                        )
                    }

                    if (state.verseRefs.isNotEmpty()) {
                        item {
                            VerseRefsCard(
                                topic = topic,
                                totalCount = topic.verseCount,
                                verseRefs = state.verseRefs,
                                previews = state.versePreviews,
                            )
                        }
                    }

                    if (state.childTopics.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = if (topic.verseCount > 0) "Go Deeper" else "Subtopics",
                                count = state.childTopics.size,
                            )
                        }

                        items(state.childTopics, key = { it.id }) { child ->
                            TopicListItem(
                                topic = child,
                                accent = if (tree == TopicsTree.Ontology) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.tertiary
                                },
                                onClick = {
                                    val (targetTopicId, targetTrail) = resolveChildTopicNavigationTarget(
                                        child = child,
                                        detailState = state,
                                        roots = roots,
                                        fallbackTrail = currentTrail + topic.id,
                                    )
                                    navController.navigate(
                                        QuranicTopicRoutes.topic(
                                            tree = tree,
                                            topicId = targetTopicId,
                                            trail = targetTrail,
                                        ),
                                        topicsNavOptions()
                                    )
                                },
                            )
                        }
                    }

                    if (state.broaderCatalogChildren.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "More from broader catalog",
                                count = state.broaderCatalogChildren.size,
                            )
                        }

                        items(state.broaderCatalogChildren, key = { "broader_${it.id}" }) { child ->
                            TopicListItem(
                                topic = child,
                                accent = if (tree == TopicsTree.Ontology) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.tertiary
                                },
                                onClick = {
                                    val (targetTopicId, targetTrail) = resolveChildTopicNavigationTarget(
                                        child = child,
                                        detailState = state,
                                        roots = roots,
                                        fallbackTrail = currentTrail + topic.id,
                                    )
                                    navController.navigate(
                                        QuranicTopicRoutes.topic(
                                            tree = tree,
                                            topicId = targetTopicId,
                                            trail = targetTrail,
                                        ),
                                        topicsNavOptions()
                                    )
                                },
                            )
                        }
                    }

                    if (state.relationships.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Connected Concepts",
                                count = state.relationships.size,
                            )
                        }

                        items(
                            items = state.relationships,
                            key = { "${it.type}_${it.topic.id}" },
                        ) { relationship ->
                            RelationshipItem(
                                relationship = relationship,
                                onClick = {
                                    navController.navigate(
                                        QuranicTopicRoutes.topic(
                                            tree = tree,
                                            topicId = relationship.topic.id,
                                            trail = currentTrail + topic.id,
                                        ),
                                        topicsNavOptions()
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveChildTopicNavigationTarget(
    child: TopicNode,
    detailState: TopicDetailUiState,
    roots: List<TopicNode>,
    fallbackTrail: List<Int>,
): Pair<Int, List<Int>> {
    if (child.childCount > 0 || child.verseCount > 0) {
        return child.id to fallbackTrail
    }

    val candidates = buildList {
        addAll(detailState.breadcrumbs)
        detailState.topic?.let(::add)
        addAll(roots)
        addAll(detailState.childTopics)
        addAll(detailState.broaderCatalogChildren)
        addAll(detailState.relationships.map { it.topic })
    }

    val normalizedChildTitle = normalizeTopicLabel(child.title)
    if (normalizedChildTitle.isBlank()) return child.id to fallbackTrail

    val target = candidates
        .asSequence()
        .filter { it.id != child.id }
        .distinctBy { it.id }
        .sortedByDescending { normalizeTopicLabel(it.title).length }
        .firstOrNull { candidate ->
            val normalizedCandidate = normalizeTopicLabel(candidate.title)
            normalizedCandidate.isNotBlank() &&
                    (
                            normalizedChildTitle == normalizedCandidate ||
                                    Regex("\\b${Regex.escape(normalizedCandidate)}\\b")
                                        .containsMatchIn(normalizedChildTitle)
                            )
        }
        ?: return child.id to fallbackTrail

    return target.id to emptyList()
}

private fun normalizeTopicLabel(value: String): String {
    return value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
