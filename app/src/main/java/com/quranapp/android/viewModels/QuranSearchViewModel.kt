package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.db.search.SearchHistoryEntry
import com.quranapp.android.db.search.SearchHistoryStore
import com.quranapp.android.search.CollectionSearchResult
import com.quranapp.android.search.ExclusiveVersesSearchProvider
import com.quranapp.android.search.QuickLinkItem
import com.quranapp.android.search.SearchPagingSource
import com.quranapp.android.search.SearchQuickLinksParser
import com.quranapp.android.search.SearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class QuranSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatabaseProvider.getQuranRepository(application)
    private val searchHistoryStore = SearchHistoryStore(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _quranTextEnabled = MutableStateFlow(false)
    val quranTextEnabled: StateFlow<Boolean> = _quranTextEnabled

    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryEntry>> = _searchHistory

    private val debouncedQuery = _searchQuery
        .debounce(200)
        .distinctUntilChanged()
        .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val quickLinks: StateFlow<List<QuickLinkItem>> = debouncedQuery
        .mapLatest { query ->
            SearchQuickLinksParser.parse(repository, query)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList(),
        )

    val surahResults: StateFlow<List<SurahWithLocalizations>?> = debouncedQuery
        .mapLatest { query ->
            repository.searchSurahs(query)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null,
        )

    val topicResults: StateFlow<List<CollectionSearchResult>> = debouncedQuery
        .mapLatest { query ->
            ExclusiveVersesSearchProvider.search(getApplication(), query)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList(),
        )

    val searchResults: Flow<PagingData<SearchResult>> = debouncedQuery
        .combine(quranTextEnabled) { query, sourceQuran ->
            query to sourceQuran
        }
        .flatMapLatest { (query, sourceQuran) ->
            if (query.isBlank()) {
                return@flatMapLatest flowOf(PagingData.empty())
            }

            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                )
            ) {
                SearchPagingSource(
                    application = getApplication(),
                    query = query,
                    sourceQuran = sourceQuran,
                )
            }.flow
        }
        .cachedIn(viewModelScope)
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    fun onQueryChange(value: String, isQuranText: Boolean = false) {
        _searchQuery.value = value

        if (isQuranText) {
            _quranTextEnabled.value = true
        }
    }

    fun toggleQuranTextEnabled(postRun: (Boolean) -> Unit) {
        val newValue = !_quranTextEnabled.value
        _quranTextEnabled.value = newValue

        postRun(newValue)
    }

    fun refreshSearchHistory() {
        viewModelScope.launch {
            _searchHistory.value = searchHistoryStore.loadAll()
        }
    }

    // Call when the user commits a search outcome
    fun recordSearchQuery(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            searchHistoryStore.add(trimmed)
            _searchHistory.value = searchHistoryStore.loadAll()
        }
    }

    fun recordCurrentSearchQuery() {
        recordSearchQuery(_searchQuery.value)
    }

    fun removeSearchHistory(id: Int) {
        viewModelScope.launch {
            searchHistoryStore.remove(id)
            _searchHistory.value = searchHistoryStore.loadAll()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.clear()
            _searchHistory.value = emptyList()
        }
    }

    fun historySuggestionsForDisplay(
        query: String,
        quickLinks: List<QuickLinkItem>,
    ): List<SearchHistoryEntry> {
        val q = query.trim()
        if (q.isEmpty() || quickLinks.isNotEmpty()) return emptyList()
        val ql = q.lowercase()
        val filtered = _searchHistory.value
            .asSequence()
            .filter { it.text.lowercase() != ql }
            .filter { it.text.contains(q, ignoreCase = true) }
            .toList()
        val prefix = filtered.filter { it.text.startsWith(q, ignoreCase = true) }
        val rest = filtered.filter { !it.text.startsWith(q, ignoreCase = true) }
        return (prefix + rest).distinctBy { it.id }.take(5)
    }
}
