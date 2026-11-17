package com.quranapp.android.screens.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.components.collections.CreateUpdateCollectionSheet
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.SearchTextField
import com.quranapp.android.components.library.CollectionsEmptyState
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.extensions.predictivePopEnterTransition
import com.quranapp.android.extensions.predictivePopExitTransition
import com.quranapp.android.extensions.route
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.create_new_collection
import com.quranapp.android.generated.resources.dr_icon_search
import com.quranapp.android.generated.resources.my_collections
import com.quranapp.android.generated.resources.noResults
import com.quranapp.android.generated.resources.strHintSearch
import com.quranapp.android.navigation.CollectionRoutes
import com.quranapp.android.shared.R
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.viewmodel.CollectionsViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.res.painterResource as androidPainterResource

@Composable
fun CollectionsScreen(
    initialCollectionId: Long? = null,
    vm: CollectionsViewModel = appViewModel(),
) {
    val navController = rememberNavController()

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = initialCollectionId?.let { CollectionRoutes.DETAIL.arg(it) }
            ?: CollectionRoutes.HOME,
        predictivePopEnterTransition = predictivePopEnterTransition,
        predictivePopExitTransition = predictivePopExitTransition,
    ) {
        route(CollectionRoutes.HOME) {
            CollectionsHome(
                vm = vm,
                onOpenCollection = { id ->
                    navController.navigate(CollectionRoutes.DETAIL.arg(id))
                },
            )
        }
        route(
            route = CollectionRoutes.DETAIL(),
            arguments = listOf(
                navArgument(Keys.COLLECTION_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            CollectionDetailScreen(
                vm = vm,
                collectionId = entry.arguments?.getLong(Keys.COLLECTION_ID) ?: 0L,
            )
        }
    }
}

@Composable
private fun CollectionsHome(
    vm: CollectionsViewModel,
    onOpenCollection: (Long) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var hasCompletedInitialLoad by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(300.milliseconds)
        debouncedQuery = query
    }

    val collections = remember(debouncedQuery) {
        vm.collections(debouncedQuery)
    }.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.my_collections),
            )
        },
        bottomBar = {
            if (collections.itemCount != 0) {
                FilledTonalButton(
                    onClick = {
                        showCreate = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                ) {
                    Icon(
                        painter = androidPainterResource(R.drawable.media3_icon_plus),
                        contentDescription = null,
                    )
                    Text(stringResource(Res.string.create_new_collection))
                }
            }
        }
    ) { padding ->
        val isRefreshing = collections.loadState.refresh is LoadState.Loading

        LaunchedEffect(isRefreshing) {
            if (!isRefreshing) hasCompletedInitialLoad = true
        }

        when {
            isRefreshing && !hasCompletedInitialLoad -> Box(Modifier.padding(padding)) {
                Loader(
                    fill = true
                )
            }

            collections.itemCount == 0 &&
                    query.isBlank() &&
                    debouncedQuery.isBlank() &&
                    !isRefreshing -> CollectionsEmptyState(
                modifier = Modifier.padding(padding),
                onCreateCollection = {
                    showCreate = true
                },
            )

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    val columns = when {
                        maxWidth >= 840.dp -> 3
                        maxWidth >= 600.dp -> 2
                        else -> 1
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        stickyHeader {
                            SearchTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.background),
                                placeholder = stringResource(Res.string.strHintSearch),
                                leadingIcon = {
                                    Icon(
                                        painterResource(Res.drawable.dr_icon_search),
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        if ((isRefreshing || query != debouncedQuery) && collections.itemCount == 0) {
                            item(key = "search-loader", span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Loader()
                                }
                            }
                        } else if (collections.itemCount == 0) {
                            item(key = "no-results", span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.noResults),
                                        style = typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(
                                count = collections.itemCount,
                                key = { index ->
                                    collections.peek(index)?.collection?.id?.let { "col-$it" }
                                        ?: "col-index-$index"
                                },
                            ) { index ->
                                val item = collections[index] ?: return@items

                                CollectionCard(
                                    item = item,
                                    onClick = { onOpenCollection(item.collection.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CreateUpdateCollectionSheet(
        isOpen = showCreate,
        onClose = {
            showCreate = false
        },
    )
}
