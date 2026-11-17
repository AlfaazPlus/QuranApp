package com.quranapp.android.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.components.search.ExclusiveSearchResults
import com.quranapp.android.components.search.QuickLinks
import com.quranapp.android.components.search.SearchEmptyScrollContent
import com.quranapp.android.components.search.SearchFiltersSheet
import com.quranapp.android.components.search.SearchHistorySuggestionStrip
import com.quranapp.android.components.search.SurahSearchResults
import com.quranapp.android.components.search.TextSearchResults
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.clear
import com.quranapp.android.generated.resources.dr_icon_close
import com.quranapp.android.generated.resources.dr_icon_filter
import com.quranapp.android.generated.resources.dr_icon_mic
import com.quranapp.android.generated.resources.dr_icon_quran_script
import com.quranapp.android.generated.resources.results
import com.quranapp.android.generated.resources.searchIndexingInProgress
import com.quranapp.android.generated.resources.searchTipArabic
import com.quranapp.android.generated.resources.strHintSearch
import com.quranapp.android.generated.resources.strHintSearchQuran
import com.quranapp.android.generated.resources.strLabelClose
import com.quranapp.android.generated.resources.strLabelGotIt
import com.quranapp.android.generated.resources.strLabelOff
import com.quranapp.android.generated.resources.strLabelOn
import com.quranapp.android.generated.resources.strTitleFilters
import com.quranapp.android.generated.resources.strTitleInfo
import com.quranapp.android.generated.resources.strTitleReaderChapters
import com.quranapp.android.generated.resources.titleGlobalSearch
import com.quranapp.android.generated.resources.topics
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.search.SearchResult
import com.quranapp.android.theme.alpha
import com.quranapp.android.viewmodel.QuranSearchViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private fun LazyPagingItems<SearchResult>.resultsCountLabel(): String? {
    if (loadState.refresh is LoadState.Loading) return null

    val count = itemCount
    val initialLoadSize = QuranSearchViewModel.SEARCH_PAGE_SIZE
    val hasMoreThanInitialLoad =
        count >= initialLoadSize && !loadState.append.endOfPaginationReached
    return if (hasMoreThanInitialLoad) "$initialLoadSize+" else count.toString()
}

private enum class SearchResultTab {
    results, chapters, topics,
}

@Composable
fun SearchScreen(
    supportsVoiceSearch: Boolean,
    voiceSearchFlow: SharedFlow<String>,
    onVoiceSearchClick: (inQuranText: Boolean) -> Unit,
) {
    val viewModel = appViewModel<QuranSearchViewModel>()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val isSearchIndexing by viewModel.isSearchIndexing.collectAsState()
    var showIndexingMsg by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSearchHistory()
    }

    LaunchedEffect(Unit) {
        voiceSearchFlow.collect { viewModel.onQueryChange(it) }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(Res.string.titleGlobalSearch), actions = {
                    if (isSearchIndexing) {
                        Box(
                            Modifier.clickable {
                                showIndexingMsg = true
                            }) {
                            Loader()
                        }
                    }

                    if (supportsVoiceSearch) {
                        IconButton(
                            onClick = {
                                onVoiceSearchClick(viewModel.quranTextEnabled.value)
                            }) {
                            Icon(
                                painterResource(Res.drawable.dr_icon_mic),
                                contentDescription = stringResource(Res.string.strHintSearch),
                            )
                        }
                    }

                    val activeFilters by viewModel.currentFilters.collectAsState()

                    IconButton(
                        onClick = { showFilterSheet = true },
                    ) {
                        Box {
                            Icon(
                                painterResource(Res.drawable.dr_icon_filter),
                                contentDescription = stringResource(Res.string.strTitleFilters),
                            )
                            if (!activeFilters.isEmpty) {
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).size(8.dp)
                                        .background(colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }, shadowElevation = 0.dp
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            SearchBox(viewModel)

            val query by viewModel.searchQuery.collectAsState()
            val quickLinks by viewModel.quickLinks.collectAsState()
            val searchHistory by viewModel.searchHistory.collectAsState()

            val historySuggestions = remember(query, quickLinks, searchHistory) {
                viewModel.historySuggestionsForDisplay(query, quickLinks)
            }

            SearchHistorySuggestionStrip(
                suggestions = historySuggestions,
                onSelect = { text ->
                    viewModel.commitSpecificQuery(text)
                    viewModel.onQueryChange(text)
                },
            )

            QuickLinks(viewModel)

            TabbedResults(viewModel)
        }
    }

    val currentFilters by viewModel.currentFilters.collectAsState()
    val availableTranslations by viewModel.availableTranslations.collectAsState()
    val quranTextEnabledState by viewModel.quranTextEnabled.collectAsState()

    SearchFiltersSheet(
        isOpen = showFilterSheet,
        onDismiss = { showFilterSheet = false },
        currentFilters = currentFilters,
        availableTranslations = availableTranslations,
        quranTextEnabled = quranTextEnabledState,
        onApplyFilters = {
            viewModel.setFilters(it)
            showFilterSheet = false
        })

    AlertDialog(
        isOpen = showIndexingMsg,
        onClose = { showIndexingMsg = false },
        title = stringResource(Res.string.strTitleInfo),
        actions = listOf(
            AlertDialogAction(
                stringResource(Res.string.strLabelGotIt),
            )
        )
    ) {
        Text(stringResource(Res.string.searchIndexingInProgress))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBox(viewModel: QuranSearchViewModel) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var hasFocus by remember { mutableStateOf(false) }
    var skipNextImeHideCommit by remember { mutableStateOf(false) }
    val bgColor = colorScheme.background
    val imeVisible = WindowInsets.isImeVisible

    val query by viewModel.searchQuery.collectAsState()
    val quranTextEnabled by viewModel.quranTextEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(imeVisible, hasFocus) {
        if (imeVisible) {
            skipNextImeHideCommit = false
            return@LaunchedEffect
        }

        if (hasFocus) {
            if (skipNextImeHideCommit) {
                skipNextImeHideCommit = false
            } else {
                viewModel.commitOnExplicitSubmit()
            }
        }
    }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().background(
            color = colorScheme.surfaceContainer,
        ).padding(
            start = 16.dp, end = 16.dp, bottom = 16.dp
        ).border(
            width = 1.dp,
            color = if (hasFocus) colorScheme.outline.alpha(0.75f) else colorScheme.outline.alpha(
                0.4f
            ),
            shape = shapes.medium
        ).focusRequester(focusRequester).focusable().onFocusChanged {
            hasFocus = it.hasFocus

            if (it.isFocused) {
                keyboardController?.show()
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = bgColor,
            focusedContainerColor = bgColor,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        ),
        placeholder = {
            Text(
                stringResource(
                    if (quranTextEnabled) Res.string.searchTipArabic else Res.string.strHintSearchQuran,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onBackground.alpha(0.6f)
            )
        },
        trailingIcon = {
            Row() {
                if (query.isNotEmpty()) {
                    SimpleTooltip(text = stringResource(Res.string.clear)) {
                        IconButton(
                            onClick = { viewModel.onQueryChange("") },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_close),
                                contentDescription = stringResource(Res.string.strLabelClose),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                val arabicSearchTip = stringResource(Res.string.searchTipArabic)
                SimpleTooltip(text = arabicSearchTip) {
                    IconButton(
                        onClick = {
                            viewModel.toggleQuranTextEnabled() { enabled ->
                                scope.launch {
                                    val stateLabel = getString(
                                        if (enabled) Res.string.strLabelOn else Res.string.strLabelOff,
                                    )
                                    PlatformServices.messages.show(
                                        "$stateLabel: ${getString(Res.string.searchTipArabic)}",
                                        long = true,
                                    )
                                }
                            }
                        }, colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (quranTextEnabled) colorScheme.primary else LocalContentColor.current
                        ), shape = shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_quran_script),
                            contentDescription = arabicSearchTip,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        value = query,
        onValueChange = viewModel::onQueryChange,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                viewModel.commitOnExplicitSubmit()
                skipNextImeHideCommit = true
                keyboardController?.hide()
            }),
        textStyle = MaterialTheme.typography.labelLarge,
        shape = MaterialTheme.shapes.medium,
        singleLine = true,
    )
}

@Composable
private fun ColumnScope.TabbedResults(viewModel: QuranSearchViewModel) {
    val query by viewModel.searchQuery.collectAsState()

    val scope = rememberCoroutineScope()

    val tabs = remember {
        listOf(
            SearchResultTab.results,
            SearchResultTab.chapters,
            SearchResultTab.topics,
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )

    if (query.isBlank()) {
        SearchEmptyScrollContent(viewModel, Modifier.weight(1f))
        return
    }

    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val filterState by viewModel.currentFilters.collectAsState()
    val quranTextSearch by viewModel.quranTextEnabled.collectAsState()

    LaunchedEffect(filterState, quranTextSearch) {
        searchResults.refresh()
    }

    val surahResults by viewModel.surahResults.collectAsState()
    val exclusiveResults by viewModel.topicResults.collectAsState()

    SearchResultTabs(
        tabs,
        counts = mapOf(
            SearchResultTab.results to searchResults.resultsCountLabel(),
            SearchResultTab.chapters to surahResults?.size?.toString(),
            SearchResultTab.topics to exclusiveResults.size.toString(),
        ),
        selectedTabIndex = pagerState.currentPage,
    ) {
        scope.launch {
            pagerState.animateScrollToPage(it)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f).fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> TextSearchResults(viewModel, searchResults, !filterState.isEmpty)
            1 -> SurahSearchResults(viewModel, surahResults)
            2 -> ExclusiveSearchResults(viewModel, exclusiveResults)
        }
    }
}

@Composable
private fun SearchResultTabs(
    tabs: List<SearchResultTab>,
    counts: Map<SearchResultTab, String?>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().background(colorScheme.surfaceContainer),
        shadowElevation = 2.dp,
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorScheme.surfaceContainer,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index

                Tab(
                    selected = isSelected,
                    selectedContentColor = colorScheme.primary,
                    unselectedContentColor = colorScheme.onSurfaceVariant,
                    onClick = { onTabSelected(index) },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    when (tab) {
                                        SearchResultTab.results -> Res.string.results
                                        SearchResultTab.chapters -> Res.string.strTitleReaderChapters
                                        SearchResultTab.topics -> Res.string.topics
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val count = counts[tab]

                            if (count != null) {
                                Badge(
                                    containerColor = Color.Gray.alpha(0.5f),
                                    contentColor = colorScheme.onBackground
                                ) {
                                    Text(count)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
