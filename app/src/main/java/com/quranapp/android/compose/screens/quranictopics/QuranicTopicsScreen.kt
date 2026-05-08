package com.quranapp.android.compose.screens.quranictopics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.quranapp.android.compose.screens.settings.route
import com.quranapp.android.viewModels.TopicDetailUiState
import com.quranapp.android.viewModels.TopicsTree
import com.quranapp.android.viewModels.QuranicTopicsViewModel
import com.quranapp.android.viewModels.buildTopicDetailKey

enum class QuranicTopicsStart {
    Ontology,
    Thematic,
}

internal object QuranicTopicRoutes {
    const val ONTOLOGY = "ontology"
    const val THEMATIC = "thematic"
    const val TOPIC = "topic/{tree}/{topicId}?trail={trail}"
    const val TOPIC_SEARCH = "topic_search/{tree}"

    fun topic(tree: TopicsTree, topicId: Int, trail: List<Int> = emptyList()): String =
        "topic/${tree.routeName}/$topicId?trail=${trail.joinToString(",")}"

    fun topicSearch(tree: TopicsTree): String = "topic_search/${tree.routeName}"
}

val LocalTopicsNavController = compositionLocalOf<NavHostController> {
    error("NavHostController is not provided")
}

fun topicsNavOptions(optionsBuilder: (NavOptionsBuilder.() -> Unit)? = null) =
    navOptions {
        restoreState = true
        optionsBuilder?.invoke(this)
    }

@Composable
fun QuranicTopicsScreen(
    start: QuranicTopicsStart,
    initialTopicId: Int? = null,
    initialTrail: List<Int> = emptyList(),
    viewModel: QuranicTopicsViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalTopicsNavController provides navController) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = initialTopicId?.let { topicId ->
                    QuranicTopicRoutes.topic(
                        tree = when (start) {
                            QuranicTopicsStart.Thematic -> TopicsTree.Thematic
                            QuranicTopicsStart.Ontology -> TopicsTree.Ontology
                        },
                        topicId = topicId,
                        trail = initialTrail,
                    )
                } ?: when (start) {
                    QuranicTopicsStart.Thematic -> QuranicTopicRoutes.THEMATIC
                    else -> QuranicTopicRoutes.ONTOLOGY
                },
            ) {
                route(QuranicTopicRoutes.ONTOLOGY) {
                    QuranicTopicListRoute(
                        title = "Ontology Explorer",
                        tree = TopicsTree.Ontology,
                        isLoading = uiState.isLoadingRoots,
                        topics = uiState.ontologyRoots,
                        primaryTopicCount = uiState.ontologyPrimaryRootCount,
                        hasMoreSupplementalPages = uiState.hasMoreOntologySupplemental,
                        isLoadingMoreSupplemental = uiState.isLoadingMoreOntologySupplemental,
                        onLoadMoreSupplemental = { viewModel.loadMoreSupplementalRoots(TopicsTree.Ontology) },
                        onOpenSearch = {
                            navController.navigate(
                                QuranicTopicRoutes.topicSearch(TopicsTree.Ontology),
                                navOptions = topicsNavOptions()
                            )
                        },
                    )
                }
                route(QuranicTopicRoutes.THEMATIC) {
                    QuranicTopicListRoute(
                        title = "Thematic Topics",
                        tree = TopicsTree.Thematic,
                        isLoading = uiState.isLoadingRoots,
                        topics = uiState.thematicRoots,
                        primaryTopicCount = uiState.thematicPrimaryRootCount,
                        hasMoreSupplementalPages = uiState.hasMoreThematicSupplemental,
                        isLoadingMoreSupplemental = uiState.isLoadingMoreThematicSupplemental,
                        onLoadMoreSupplemental = { viewModel.loadMoreSupplementalRoots(TopicsTree.Thematic) },
                        onOpenSearch = {
                            navController.navigate(
                                QuranicTopicRoutes.topicSearch(TopicsTree.Thematic),
                                navOptions = topicsNavOptions()
                            )
                        },
                    )
                }
                route(
                    route = QuranicTopicRoutes.TOPIC_SEARCH,
                    arguments = listOf(
                        navArgument("tree") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val tree = TopicsTree.fromRouteName(entry.arguments?.getString("tree"))

                    QuranicTopicsSearchRoute(
                        tree = tree,
                        onSearchTopics = { query -> viewModel.searchTopicsForTree(query, tree) },
                    )
                }
                route(
                    route = QuranicTopicRoutes.TOPIC,
                    arguments = listOf(
                        navArgument("tree") { type = NavType.StringType },
                        navArgument("topicId") { type = NavType.IntType },
                        navArgument("trail") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) { entry ->
                    val tree = TopicsTree.fromRouteName(entry.arguments?.getString("tree"))
                    val topicId = entry.arguments?.getInt("topicId") ?: 0

                    val breadcrumbIds = entry.arguments
                        ?.getString("trail")
                        .orEmpty()
                        .split(',')
                        .mapNotNull { it.toIntOrNull() }

                    val detailKey = buildTopicDetailKey(tree, topicId, breadcrumbIds)
                    val detailState = uiState.topicDetails[detailKey]
                        ?: TopicDetailUiState(isLoading = topicId > 0)

                    LaunchedEffect(topicId, tree, breadcrumbIds) {
                        if (topicId > 0) {
                            viewModel.loadTopic(topicId, tree, breadcrumbIds)
                        }
                    }

                    QuranicTopicDetailRoute(
                        state = detailState,
                        roots = if (tree == TopicsTree.Ontology) uiState.ontologyRoots else uiState.thematicRoots,
                        tree = tree,
                        topicId = topicId,
                    )
                }
            }
        }
    }
}
