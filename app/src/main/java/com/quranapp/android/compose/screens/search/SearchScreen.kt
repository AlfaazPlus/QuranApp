package com.quranapp.android.compose.screens.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.search.ExclusiveSearchResults
import com.quranapp.android.compose.components.search.QuickLinks
import com.quranapp.android.compose.components.search.SearchEmptyScrollContent
import com.quranapp.android.compose.components.search.SearchHistorySuggestionStrip
import com.quranapp.android.compose.components.search.SurahSearchResults
import com.quranapp.android.compose.components.search.TextSearchResults
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.QuranSearchViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    supportsVoiceSearch: Boolean,
    voiceSearchFlow: SharedFlow<String>,
    onVoiceSearchClick: () -> Unit,
) {
    val viewModel = viewModel<QuranSearchViewModel>()

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
                title = stringResource(R.string.titleGlobalSearch),
                actions = {
                    if (supportsVoiceSearch) {
                        IconButton(onClick = onVoiceSearchClick) {
                            Icon(
                                painterResource(R.drawable.dr_icon_mic),
                                contentDescription = stringResource(R.string.strHintSearch),
                            )
                        }
                    }
                },
                shadowElevation = 0.dp
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                    viewModel.recordSearchQuery(text)
                    viewModel.onQueryChange(text)
                },
            )

            QuickLinks(viewModel)

            TabbedResults(viewModel)
        }
    }
}

@Composable
private fun SearchBox(viewModel: QuranSearchViewModel) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var hasFocus by remember { mutableStateOf(false) }
    val bgColor = colorScheme.background

    val query by viewModel.searchQuery.collectAsState()
    val quranTextEnabled by viewModel.quranTextEnabled.collectAsState()

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorScheme.surfaceContainer,
            )
            .padding(
                start = 16.dp, end = 16.dp, bottom = 16.dp
            )
            .border(
                width = 1.dp,
                color = if (hasFocus) colorScheme.outline.alpha(0.75f) else colorScheme.outline.alpha(
                    0.4f
                ),
                shape = shapes.medium
            )
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged {
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
                stringResource(R.string.strHintSearchQuran),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onBackground.alpha(0.6f)
            )
        },
        trailingIcon = {
            Row() {
                if (query.isNotEmpty()) {
                    SimpleTooltip(text = stringResource(R.string.clear)) {
                        IconButton(
                            onClick = { viewModel.onQueryChange("") },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_close),
                                contentDescription = stringResource(R.string.strLabelClose),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                SimpleTooltip(text = stringResource(R.string.searchTipArabic)) {
                    IconButton(
                        onClick = {
                            viewModel.toggleQuranTextEnabled() {
                                val stateResInt =
                                    if (it) R.string.strLabelOn else R.string.strLabelOff

                                MessageUtils.showRemovableToast(
                                    context,
                                    "${resources.getString(stateResInt)}: ${
                                        resources.getString(
                                            R.string.searchTipArabic
                                        )
                                    }",
                                    Toast.LENGTH_LONG
                                )
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (quranTextEnabled) colorScheme.primary else LocalContentColor.current
                        ),
                        shape = shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_quran_script),
                            contentDescription = stringResource(R.string.searchTipArabic),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        value = query,
        onValueChange = viewModel::onQueryChange,
        textStyle = MaterialTheme.typography.titleSmall,
        shape = MaterialTheme.shapes.medium,
        singleLine = true,
    )
}

private enum class SearchResultTab {
    results, chapters, topics,
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

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect {
                viewModel.recordCurrentSearchQuery()
            }
    }

    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val surahResults by viewModel.surahResults.collectAsState()
    val exclusiveResults by viewModel.topicResults.collectAsState()

    SearchResultTabs(
        tabs,
        counts = mapOf(
            SearchResultTab.results to if (searchResults.loadState.refresh is LoadState.Loading) null else searchResults.itemCount,
            SearchResultTab.chapters to surahResults?.size,
            SearchResultTab.topics to exclusiveResults.size,
        ),
        selectedTabIndex = pagerState.currentPage,
    ) {
        scope.launch {
            pagerState.animateScrollToPage(it)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> TextSearchResults(viewModel, searchResults)
            1 -> SurahSearchResults(viewModel, surahResults)
            2 -> ExclusiveSearchResults(viewModel, exclusiveResults)
        }
    }
}


@Composable
private fun SearchResultTabs(
    tabs: List<SearchResultTab>,
    counts: Map<SearchResultTab, Int?>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer),
        shadowElevation = 2.dp,
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorScheme.surfaceContainer,
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
                                        SearchResultTab.results -> R.string.results
                                        SearchResultTab.chapters -> R.string.strTitleReaderChapters
                                        SearchResultTab.topics -> R.string.topics
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val count = counts[tab]

                            if (count != null) {
                                Badge(
                                    containerColor = Color.Gray.alpha(0.5f),
                                    contentColor = colorScheme.onBackground
                                ) {
                                    Text("$count")
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
