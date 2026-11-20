package com.quranapp.android.screens.reader

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.common.CenteredSecondaryScrollableTabRow
import com.quranapp.android.components.common.Chip
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.components.reader.navigator.ChapterCard
import com.quranapp.android.components.reader.navigator.FilterField
import com.quranapp.android.components.reader.navigator.HizbCard
import com.quranapp.android.components.reader.navigator.JuzCard
import com.quranapp.android.db.relations.NavigationUnit
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.any
import com.quranapp.android.generated.resources.chapterFilterLengthLong
import com.quranapp.android.generated.resources.chapterFilterLengthMedium
import com.quranapp.android.generated.resources.chapterFilterLengthShort
import com.quranapp.android.generated.resources.chapterFilters
import com.quranapp.android.generated.resources.clearFilters
import com.quranapp.android.generated.resources.dr_icon_chevron_left
import com.quranapp.android.generated.resources.dr_icon_filter
import com.quranapp.android.generated.resources.dr_icon_search
import com.quranapp.android.generated.resources.dr_icon_sort
import com.quranapp.android.generated.resources.favourites
import com.quranapp.android.generated.resources.filterSectionLength
import com.quranapp.android.generated.resources.icon_star_outlined
import com.quranapp.android.generated.resources.msgNoFavouriteChapters
import com.quranapp.android.generated.resources.quran_kareem
import com.quranapp.android.generated.resources.sajda
import com.quranapp.android.generated.resources.strHintSearch
import com.quranapp.android.generated.resources.strHintSearchBy
import com.quranapp.android.generated.resources.strHintSearchChapter
import com.quranapp.android.generated.resources.strHintSearchHizb
import com.quranapp.android.generated.resources.strLabelBack
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelDone
import com.quranapp.android.generated.resources.strLabelRemove
import com.quranapp.android.generated.resources.strTitleChapInfoRevType
import com.quranapp.android.generated.resources.strTitleFilters
import com.quranapp.android.generated.resources.strTitleHolyQuran
import com.quranapp.android.generated.resources.strTitleMadani
import com.quranapp.android.generated.resources.strTitleMakki
import com.quranapp.android.generated.resources.strTitleReaderChapters
import com.quranapp.android.generated.resources.strTitleReaderHizb
import com.quranapp.android.generated.resources.strTitleReaderJuz
import com.quranapp.android.generated.resources.titleRemoveFromFavourites
import com.quranapp.android.generated.resources.withSajda
import com.quranapp.android.generated.resources.withoutSajda
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.theme.tightTextStyle
import com.quranapp.android.utils.reader.ReaderChapterIndexFilters
import com.quranapp.android.utils.reader.ReaderChapterLengthFilter
import com.quranapp.android.utils.reader.ReaderChapterRevelationFilter
import com.quranapp.android.utils.reader.ReaderChapterSajdaFilter
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.filteredByChapterIndex
import com.quranapp.android.viewmodel.ReaderIndexViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val ReaderIndexExpandedHeaderHeight = 220.dp
private val ReaderIndexCollapsedHeaderHeight = 84.dp
private val ReaderIndexTabHeight = 48.dp

private enum class ReaderIndexTab {
    chapters,
    juz,
    hizb,
    favourites
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderIndexScreen() {
    val density = LocalDensity.current
    val viewModel = appViewModel<ReaderIndexViewModel>()

    val surahs by viewModel.surahs.collectAsState()
    val juzs by viewModel.juzs.collectAsState()
    val hizbs by viewModel.hizbs.collectAsState()

    val tabs = remember {
        listOf(
            ReaderIndexTab.chapters,
            ReaderIndexTab.juz,
            ReaderIndexTab.hizb,
            ReaderIndexTab.favourites,
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val listReversed = remember { mutableStateMapOf<ReaderIndexTab, Boolean>() }

    val chaptersListState = rememberLazyGridState()
    val juzListState = rememberLazyGridState()
    val hizbListState = rememberLazyGridState()
    val favListState = rememberLazyGridState()

    val scope = rememberCoroutineScope()

    val topAppBarState = rememberTopAppBarState(
        initialHeightOffsetLimit = with(density) {
            -(ReaderIndexExpandedHeaderHeight - ReaderIndexCollapsedHeaderHeight).toPx()
        }
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        snapAnimationSpec = null
    )

    val selectedTab = tabs[pagerState.currentPage]

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.background,
        topBar = {
            ReaderIndexTopBar(scrollBehavior)
        },
        floatingActionButton = {
            ReaderIndexSortFab(
                visible = selectedTab != ReaderIndexTab.favourites,
                onClick = {
                    listReversed[selectedTab] =
                        !listReversed.getOrDefault(selectedTab, false)

                    scope.launch {
                        when (selectedTab) {
                            ReaderIndexTab.chapters -> chaptersListState.scrollToItem(0)
                            ReaderIndexTab.juz -> juzListState.scrollToItem(0)
                            ReaderIndexTab.hizb -> hizbListState.scrollToItem(0)
                            else -> {}
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> ReaderIndexChaptersList(
                            viewModel = viewModel,
                            surahs = surahs,
                            reversed = listReversed.getOrDefault(ReaderIndexTab.chapters, false),
                            listState = chaptersListState,
                            nestedScrollConnection = scrollBehavior.nestedScrollConnection
                        )

                        1 -> ReaderIndexJuzList(
                            viewModel = viewModel,
                            juzs = juzs,
                            reversed = listReversed.getOrDefault(ReaderIndexTab.juz, false),
                            listState = juzListState,
                            nestedScrollConnection = scrollBehavior.nestedScrollConnection
                        )

                        2 -> ReaderIndexHizbList(
                            viewModel = viewModel,
                            hizbs = hizbs,
                            reversed = listReversed.getOrDefault(ReaderIndexTab.hizb, false),
                            listState = hizbListState,
                            nestedScrollConnection = scrollBehavior.nestedScrollConnection
                        )

                        3 -> ReaderIndexFavChaptersList(
                            viewModel = viewModel,
                            surahs = surahs,
                            listState = favListState,
                            nestedScrollConnection = scrollBehavior.nestedScrollConnection
                        )
                    }
                }

                ReaderIndexTabs(
                    modifier = Modifier.align(Alignment.TopCenter),
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        if (index == pagerState.currentPage) {
                            scope.launch {
                                when (index) {
                                    0 -> chaptersListState.animateScrollToItem(0)
                                    1 -> juzListState.animateScrollToItem(0)
                                    2 -> hizbListState.animateScrollToItem(0)
                                    3 -> favListState.animateScrollToItem(0)
                                }
                            }
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderIndexTopBar(
    scrollBehavior: TopAppBarScrollBehavior
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = LocalContext.current
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    val appBarHeight = lerp(
        start = ReaderIndexExpandedHeaderHeight,
        stop = ReaderIndexCollapsedHeaderHeight,
        fraction = collapsedFraction
    )

    val padding = lerp(
        start = 24.dp,
        stop = 0.dp,
        fraction = collapsedFraction
    )

    val iconBottomSpacing = lerp(
        start = 8.dp,
        stop = 0.dp,
        fraction = collapsedFraction
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(appBarHeight)
            .background(colorScheme.surfaceContainer)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ReaderIndexHeader(
            padding = padding,
            iconBottomSpacing = iconBottomSpacing
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { backDispatcher?.onBackPressed() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_left),
                    contentDescription = stringResource(Res.string.strLabelBack),
                    tint = colorScheme.onSurface
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    Platform.navigator.navigate(AppRoute.Screen(AppRoutes.SEARCH))
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_search),
                    contentDescription = stringResource(Res.string.strHintSearch),
                    tint = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ReaderIndexHeader(
    padding: Dp,
    iconBottomSpacing: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.quran_kareem),
            contentDescription = stringResource(Res.string.strTitleHolyQuran),
            modifier = Modifier
                .height(100.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(iconBottomSpacing))

        Text(
            text = stringResource(Res.string.strTitleHolyQuran),
            style = MaterialTheme.typography.titleSmall.merge(tightTextStyle),
            color = colorScheme.onSurface
        )
    }
}

@Composable
private fun ReaderIndexTabs(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        Res.string.strTitleReaderChapters,
        Res.string.strTitleReaderJuz,
        Res.string.strTitleReaderHizb,
        Res.string.favourites,
    )
    val borderColor = colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()

                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            },
        shadowElevation = 2.dp,
    ) {
        CenteredSecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            tabCount = tabs.size,
            containerColor = colorScheme.surfaceContainer
        ) { index, tabModifier ->
            val isSelected = selectedTabIndex == index
            val titleRes = tabs[index]

            Tab(
                modifier = tabModifier,
                selected = isSelected,
                selectedContentColor = colorScheme.primary,
                unselectedContentColor = colorScheme.onSurfaceVariant,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderIndexChaptersList(
    viewModel: ReaderIndexViewModel,
    surahs: List<SurahWithLocalizations>,
    reversed: Boolean,
    listState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favChapters by viewModel.favouriteChapters.collectAsStateWithLifecycle()

    val chapterFilters by viewModel.chapterIndexFilters.collectAsState()
    val surahNosWithSajdah by viewModel.surahNosWithSajdah.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filteredSurahs by remember { mutableStateOf(surahs) }
    var filterSheetOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(
        searchQuery,
        surahs,
        reversed,
        chapterFilters,
        surahNosWithSajdah,
    ) {
        val query = searchQuery.lowercase().trim()
        val searched = if (query.isEmpty()) {
            surahs
        } else {
            val surahNos = viewModel.repository.searchSurahNos(query)
            surahs.filter { it.surah.surahNo in surahNos }
        }
        val filtered = searched.filteredByChapterIndex(chapterFilters, surahNosWithSajdah)
        filteredSurahs = if (reversed) filtered.reversed() else filtered
    }

    if (surahs.isEmpty()) return Loader(true)

    val isFilterApplied = !chapterFilters.isDefault()

    BoxWithConstraints {
        val cellCount = if (maxWidth < 600.dp) 1 else 2

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cellCount),
                state = listState,
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = ReaderIndexTabHeight + 16.dp,
                    bottom = 128.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterField(
                            modifier = Modifier.weight(1f),
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            hint = stringResource(Res.string.strHintSearchChapter),
                            keyboardType = KeyboardType.Text,
                        )

                        BadgedBox(
                            badge = {
                                if (isFilterApplied) {
                                    Badge()
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { filterSheetOpen = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_filter),
                                    contentDescription = stringResource(Res.string.chapterFilters),
                                    tint = if (isFilterApplied) colorScheme.primary else colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                items(filteredSurahs, key = { it.surah.surahNo }) { surah ->
                    val isFav = favChapters.contains(surah.surah.surahNo)

                    ChapterCard(
                        surah = surah,
                        isFavourite = isFav,
                        onClick = {
                            Platform.navigator.navigate(AppRoute.Reader(ReaderFactory.chapter(surah.surah.surahNo)))
                        },
                        onToggleFavourite = {
                            scope.launch {
                                if (isFav) {
                                    viewModel.removeFromFavourites(
                                        surah.surah.surahNo,
                                        favChapters
                                    )
                                } else {
                                    viewModel.addToFavourites(
                                        surah.surah.surahNo,
                                        favChapters
                                    )
                                }
                            }
                        }
                    )
                }
            }

            ReaderIndexChapterFiltersSheet(
                isOpen = filterSheetOpen,
                onDismiss = { filterSheetOpen = false },
                filters = chapterFilters,
                onSetFilters = { viewModel.setChapterIndexFilters(it) },
            )
        }
    }
}

@Composable
private fun ReaderIndexChapterFiltersSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    filters: ReaderChapterIndexFilters,
    onSetFilters: (ReaderChapterIndexFilters) -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_filter,
        title = stringResource(Res.string.strTitleFilters),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.strTitleChapInfoRevType),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    selected = filters.revelation == ReaderChapterRevelationFilter.any,
                    onClick = {
                        onSetFilters(filters.copy(revelation = ReaderChapterRevelationFilter.any))
                    },
                    label = { Text(stringResource(Res.string.any)) },
                )
                Chip(
                    selected = filters.revelation == ReaderChapterRevelationFilter.meccan,
                    onClick = {
                        onSetFilters(filters.copy(revelation = ReaderChapterRevelationFilter.meccan))
                    },
                    label = { Text(stringResource(Res.string.strTitleMakki)) },
                )
                Chip(
                    selected = filters.revelation == ReaderChapterRevelationFilter.medinan,
                    onClick = {
                        onSetFilters(filters.copy(revelation = ReaderChapterRevelationFilter.medinan))
                    },
                    label = { Text(stringResource(Res.string.strTitleMadani)) },
                )
            }

            Text(
                text = stringResource(Res.string.sajda),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    selected = filters.sajda == ReaderChapterSajdaFilter.any,
                    onClick = {
                        onSetFilters(filters.copy(sajda = ReaderChapterSajdaFilter.any))
                    },
                    label = { Text(stringResource(Res.string.any)) },
                )
                Chip(
                    selected = filters.sajda == ReaderChapterSajdaFilter.withSajda,
                    onClick = {
                        onSetFilters(filters.copy(sajda = ReaderChapterSajdaFilter.withSajda))
                    },
                    label = { Text(stringResource(Res.string.withSajda)) },
                )
                Chip(
                    selected = filters.sajda == ReaderChapterSajdaFilter.withoutSajda,
                    onClick = {
                        onSetFilters(filters.copy(sajda = ReaderChapterSajdaFilter.withoutSajda))
                    },
                    label = { Text(stringResource(Res.string.withoutSajda)) },
                )
            }

            Text(
                text = stringResource(Res.string.filterSectionLength),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    selected = filters.length == ReaderChapterLengthFilter.any,
                    onClick = {
                        onSetFilters(filters.copy(length = ReaderChapterLengthFilter.any))
                    },
                    label = { Text(stringResource(Res.string.any)) },
                )
                Chip(
                    selected = filters.length == ReaderChapterLengthFilter.short,
                    onClick = {
                        onSetFilters(filters.copy(length = ReaderChapterLengthFilter.short))
                    },
                    label = { Text(stringResource(Res.string.chapterFilterLengthShort)) },
                )
                Chip(
                    selected = filters.length == ReaderChapterLengthFilter.medium,
                    onClick = {
                        onSetFilters(filters.copy(length = ReaderChapterLengthFilter.medium))
                    },
                    label = { Text(stringResource(Res.string.chapterFilterLengthMedium)) },
                )
                Chip(
                    selected = filters.length == ReaderChapterLengthFilter.long,
                    onClick = {
                        onSetFilters(filters.copy(length = ReaderChapterLengthFilter.long))
                    },
                    label = { Text(stringResource(Res.string.chapterFilterLengthLong)) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainer)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onSetFilters(ReaderChapterIndexFilters.Default) },
            ) {
                Text(stringResource(Res.string.clearFilters))
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.strLabelDone))
            }
        }
    }
}

@Composable
private fun ReaderIndexJuzList(
    viewModel: ReaderIndexViewModel,
    juzs: List<NavigationUnit>,
    reversed: Boolean,
    listState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filteredJuzs by remember { mutableStateOf(juzs) }

    LaunchedEffect(searchQuery, juzs, reversed) {
        val query = searchQuery.lowercase().trim()
        val base = if (query.isEmpty()) {
            juzs
        } else {
            val surahNos = viewModel.repository.searchSurahNos(query)
            juzs.filter { juz ->
                juz.unitNo.toString().contains(query)
                        || juz.ranges.any { it.surah.surah.surahNo in surahNos }
            }
        }
        filteredJuzs = if (reversed) base.reversed() else base
    }

    if (juzs.isEmpty()) return Loader(true)

    BoxWithConstraints {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (maxWidth < 600.dp) 1 else 2),
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = ReaderIndexTabHeight + 16.dp,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilterField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    hint = stringResource(Res.string.strHintSearchBy),
                    keyboardType = KeyboardType.Text,
                )
            }

            items(filteredJuzs, key = { it.unitNo }) { juz ->
                JuzCard(
                    juz = juz,
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Reader(ReaderFactory.juz(juz.unitNo)))
                    },
                )
            }
        }
    }
}

@Composable
private fun ReaderIndexHizbList(
    viewModel: ReaderIndexViewModel,
    hizbs: List<NavigationUnit>,
    reversed: Boolean,
    listState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filteredHizbs by remember { mutableStateOf(hizbs) }

    LaunchedEffect(searchQuery, hizbs, reversed) {
        val query = searchQuery.lowercase().trim()
        val base = if (query.isEmpty()) {
            hizbs
        } else {
            val surahNos = viewModel.repository.searchSurahNos(query)
            hizbs.filter { hizb ->
                hizb.unitNo.toString().contains(query)
                        || hizb.ranges.any { it.surah.surah.surahNo in surahNos }
            }
        }
        filteredHizbs = if (reversed) base.reversed() else base
    }

    if (hizbs.isEmpty()) return Loader(true)

    BoxWithConstraints {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (maxWidth < 300.dp) 1 else 2),
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = ReaderIndexTabHeight + 16.dp,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilterField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    hint = stringResource(Res.string.strHintSearchHizb),
                    keyboardType = KeyboardType.Text,
                )
            }

            items(filteredHizbs, key = { it.unitNo }) { hizb ->
                HizbCard(
                    hizb = hizb,
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Reader(ReaderFactory.hizb(hizb.unitNo)))
                    }
                )
            }
        }
    }
}

@Composable
private fun ReaderIndexFavChaptersList(
    viewModel: ReaderIndexViewModel,
    surahs: List<SurahWithLocalizations>,
    listState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favChapters by viewModel.favouriteChapters.collectAsStateWithLifecycle()

    val favSurahs = remember(surahs, favChapters) {
        favChapters.mapNotNull { chapterNo ->
            surahs.find { it.surah.surahNo == chapterNo }
        }
    }

    when {
        favChapters.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_star_outlined),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = stringResource(Res.string.msgNoFavouriteChapters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        else -> {
            BoxWithConstraints {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (maxWidth < 600.dp) 1 else 2),
                    state = listState,
                    modifier = modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = ReaderIndexTabHeight + 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favSurahs, key = { it.surah.surahNo }) { surah ->
                        var confirmRemove by remember { mutableStateOf(false) }

                        ChapterCard(
                            surah = surah,
                            isFavourite = true,
                            onClick = {
                                Platform.navigator.navigate(
                                    AppRoute.Reader(
                                        ReaderFactory.chapter(
                                            surah.surah.surahNo
                                        )
                                    )
                                )
                            },
                            onToggleFavourite = { confirmRemove = true }
                        )

                        if (confirmRemove) {
                            AlertDialog(
                                isOpen = true,
                                onClose = { confirmRemove = false },
                                title = stringResource(Res.string.titleRemoveFromFavourites),
                                actions = listOf(
                                    AlertDialogAction(
                                        text = stringResource(Res.string.strLabelCancel),
                                    ),
                                    AlertDialogAction(
                                        text = stringResource(Res.string.strLabelRemove),
                                        style = AlertDialogActionStyle.Danger,
                                        onClick = {
                                            scope.launch {
                                                viewModel.removeFromFavourites(
                                                    surah.surah.surahNo,
                                                    favChapters,
                                                )
                                            }
                                        },
                                    ),
                                ),
                            ) {
                                Text(
                                    text = surah.getCurrentName(),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderIndexSortFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            modifier = Modifier.size(48.dp),
            onClick = onClick,
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_sort),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
