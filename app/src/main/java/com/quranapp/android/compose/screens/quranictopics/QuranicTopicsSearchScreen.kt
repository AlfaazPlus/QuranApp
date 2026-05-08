package com.quranapp.android.compose.screens.quranictopics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.SearchTextField
import com.quranapp.android.compose.components.quranic_topics.EmptyContent
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.repository.TopicSearchHit
import com.quranapp.android.viewModels.TopicsTree
import kotlinx.coroutines.delay

private const val SEARCH_RESULTS_LIMIT = 60

@Composable
internal fun QuranicTopicsSearchRoute(
    tree: TopicsTree,
    onSearchTopics: suspend (String) -> List<TopicSearchHit>,
) {
    val navController = LocalTopicsNavController.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var query by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<TopicSearchHit>>(emptyList()) }

    val normalizedQuery = query.trim()

    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    LaunchedEffect(normalizedQuery, tree) {
        if (normalizedQuery.isEmpty()) {
            isSearching = false
            results = emptyList()
            return@LaunchedEffect
        }

        isSearching = true

        delay(200)
        results = onSearchTopics(normalizedQuery)

        isSearching = false
    }

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            AppBar(
                title = when (tree) {
                    TopicsTree.Ontology -> "Search Ontology Topics"
                    TopicsTree.Thematic -> "Search Thematic Topics"
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.strHintSearch),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .focusRequester(focusRequester),
            )

            when {
                normalizedQuery.isEmpty() -> {
                    // noop
                }

                isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Loader()
                    }
                }

                results.isEmpty() -> EmptyContent(message = "No topic matches found")

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(results, key = { "topic_search_${it.topicId}" }) { hit ->
                            TopicResultCard(
                                hit = hit,
                                tree = tree,
                                onClick = {
                                    val targetTree = when (hit.preferredTree) {
                                        RelationshipType.THEMATIC_PARENT -> TopicsTree.Thematic
                                        else -> TopicsTree.Ontology
                                    }
                                    navController.navigate(
                                        QuranicTopicRoutes.topic(
                                            tree = targetTree,
                                            topicId = hit.topicId,
                                            trail = hit.breadcrumbIds,
                                        ),
                                        navOptions = topicsNavOptions()
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

@Composable
private fun TopicResultCard(
    hit: TopicSearchHit,
    tree: TopicsTree,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = hit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )

            Text(
                text = hit.pathLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopicMetaChip(
                    text = if (hit.ayahCount == 1) "1 verse" else "${hit.ayahCount} verses",
                )
                TopicMetaChip(
                    text = if (tree == TopicsTree.Ontology) "Ontology" else "Thematic",
                )
            }
        }
    }
}

@Composable
private fun TopicMetaChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
