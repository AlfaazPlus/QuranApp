package com.quranapp.android.compose.screens.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.R
import com.quranapp.android.compose.components.collections.CreateUpdateCollectionSheet
import com.quranapp.android.compose.components.collections.collectionColor
import com.quranapp.android.compose.components.collections.collectionIcon
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.SearchTextField
import com.quranapp.android.compose.components.library.CollectionsEmptyState
import com.quranapp.android.compose.screens.main.predictivePopEnterTransition
import com.quranapp.android.compose.screens.main.predictivePopExitTransition
import com.quranapp.android.compose.screens.settings.route
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.user.CollectionWithCount
import com.quranapp.android.utils.univ.CollectionRoutes
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.CollectionsViewModel
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun CollectionsScreen(
    initialCollectionId: Long? = null,
    vm: CollectionsViewModel = viewModel(),
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
                title = stringResource(R.string.my_collections),
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
                        .padding(24.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.media3_icon_plus),
                        contentDescription = null,
                    )
                    Text(stringResource(R.string.create_new_collection))
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
                                placeholder = stringResource(R.string.strHintSearch),
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.dr_icon_search),
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
                                        text = stringResource(R.string.noResults),
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

@Composable
fun CollectionCard(
    item: CollectionWithCount,
    onClick: () -> Unit,
) {
    val collection = item.collection
    val color = collectionColor(collection.color)

    Surface(
        onClick = onClick,
        shape = shapes.medium,
        color = colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(shapes.large)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(collectionIcon(collection.icon_key)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = pluralStringResource(
                        R.plurals.verse_count,
                        item.verseCount,
                        item.verseCount,
                    ),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )

                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = updatedLabel(collection.updatedAt),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.alpha(0.7f),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
fun updatedLabel(updatedAt: Date): String {
    var nowMillis by remember(updatedAt) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(updatedAt) {
        while (true) {
            val elapsed = (System.currentTimeMillis() - updatedAt.time).coerceAtLeast(0)
            val wait = when {
                elapsed < TimeUnit.MINUTES.toMillis(1) -> TimeUnit.MINUTES.toMillis(1) - elapsed
                elapsed < TimeUnit.HOURS.toMillis(1) -> TimeUnit.MINUTES.toMillis(1)
                else -> TimeUnit.HOURS.toMillis(1)
            }
            delay(wait.coerceAtLeast(1_000L).milliseconds)
            nowMillis = System.currentTimeMillis()
        }
    }

    val delta = (nowMillis - updatedAt.time).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta).toInt()
    val hours = TimeUnit.MILLISECONDS.toHours(delta).toInt()
    val days = TimeUnit.MILLISECONDS.toDays(delta).toInt()

    return when {
        minutes < 1 -> stringResource(R.string.updated_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.updated_minutes, minutes, minutes)
        hours < 24 -> pluralStringResource(R.plurals.updated_hours, hours, hours)
        else -> pluralStringResource(
            R.plurals.updated_days,
            days.coerceAtLeast(1),
            days.coerceAtLeast(1)
        )
    }
}
