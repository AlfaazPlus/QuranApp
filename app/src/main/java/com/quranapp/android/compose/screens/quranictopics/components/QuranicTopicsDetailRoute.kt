package com.quranapp.android.compose.screens.quranictopics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.screens.quranictopics.QuranicTopicRoutes
import com.quranapp.android.viewModels.QuranicTopicsTree
import com.quranapp.android.viewModels.QuranicTopicsUiState

@Composable
internal fun QuranicTopicDetailRoute(
    state: QuranicTopicsUiState,
    tree: QuranicTopicsTree,
    navController: NavController,
) {
    val topic = state.currentTopic
    val currentTrail = state.breadcrumbs.map { it.id }
    val listState = rememberLazyListState()

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
            state.isLoadingTopic -> Loader(true)
            topic == null -> EmptyContent(message = "Topic not found")
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
                            hasSubtopics = state.childTopics.isNotEmpty(),
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
                                accent = if (tree == QuranicTopicsTree.Ontology) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.tertiary
                                },
                                onClick = {
                                    val (targetTopicId, targetTrail) = resolveChildTopicNavigationTarget(
                                        child = child,
                                        state = state,
                                        tree = tree,
                                        fallbackTrail = currentTrail + topic.id,
                                    )
                                    navController.navigate(
                                        QuranicTopicRoutes.topic(
                                            tree = tree,
                                            topicId = targetTopicId,
                                            trail = targetTrail,
                                        )
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
                                        )
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
    child: com.quranapp.android.viewModels.QuranicTopicNode,
    state: QuranicTopicsUiState,
    tree: QuranicTopicsTree,
    fallbackTrail: List<Int>,
): Pair<Int, List<Int>> {
    if (child.childCount > 0 || child.verseCount > 0) {
        return child.id to fallbackTrail
    }

    val roots = if (tree == QuranicTopicsTree.Ontology) {
        state.ontologyRoots
    } else {
        state.thematicRoots
    }

    val candidates = buildList {
        addAll(state.breadcrumbs)
        state.currentTopic?.let(::add)
        addAll(roots)
        addAll(state.childTopics)
        addAll(state.relationships.map { it.topic })
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
