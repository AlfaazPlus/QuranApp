package com.quranapp.android.compose.screens.quranictopics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quranapp.android.compose.screens.quranictopics.components.QuranicTopicDetailRoute
import com.quranapp.android.compose.screens.quranictopics.components.QuranicTopicListRoute
import com.quranapp.android.compose.screens.settings.route
import com.quranapp.android.viewModels.QuranicTopicsTree
import com.quranapp.android.viewModels.QuranicTopicsViewModel

enum class QuranicTopicsStart {
    Ontology,
    Thematic,
}

internal object QuranicTopicRoutes {
    const val ONTOLOGY = "ontology"
    const val THEMATIC = "thematic"
    const val TOPIC = "topic/{tree}/{topicId}?trail={trail}"

    fun topic(tree: QuranicTopicsTree, topicId: Int, trail: List<Int> = emptyList()): String =
        "topic/${tree.routeName}/$topicId?trail=${trail.joinToString(",")}"
}

@Composable
fun QuranicTopicsScreen(
    start: QuranicTopicsStart,
    viewModel: QuranicTopicsViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = when (start) {
                QuranicTopicsStart.Thematic -> QuranicTopicRoutes.THEMATIC
                else -> QuranicTopicRoutes.ONTOLOGY
            },
        ) {
            route(QuranicTopicRoutes.ONTOLOGY) {
                QuranicTopicListRoute(
                    title = "Ontology Explorer",
                    tree = QuranicTopicsTree.Ontology,
                    isLoading = uiState.isLoadingRoots,
                    topics = uiState.ontologyRoots,
                    navController = navController,
                )
            }
            route(QuranicTopicRoutes.THEMATIC) {
                QuranicTopicListRoute(
                    title = "Thematic Topics",
                    tree = QuranicTopicsTree.Thematic,
                    isLoading = uiState.isLoadingRoots,
                    topics = uiState.thematicRoots,
                    navController = navController,
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
                val tree = QuranicTopicsTree.fromRouteName(entry.arguments?.getString("tree"))
                val topicId = entry.arguments?.getInt("topicId") ?: 0
                val breadcrumbIds = entry.arguments
                    ?.getString("trail")
                    .orEmpty()
                    .split(',')
                    .mapNotNull { it.toIntOrNull() }

                LaunchedEffect(topicId, tree, breadcrumbIds) {
                    if (topicId > 0) {
                        viewModel.loadTopic(topicId, tree, breadcrumbIds)
                    }
                }

                QuranicTopicDetailRoute(
                    state = uiState,
                    tree = tree,
                    navController = navController,
                )
            }
        }
    }
}
