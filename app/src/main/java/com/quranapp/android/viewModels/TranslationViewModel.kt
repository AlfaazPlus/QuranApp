package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.managers.TranslationDownloadManager
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.repository.TranslationRepository
import com.quranapp.android.utils.reader.translation.TranslationVersionManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TranslationUiState(
    val isLoading: Boolean = true,
    val translationGroups: List<TranslationGroupModel> = emptyList(),
    val selectedSlugs: Set<String> = emptySet(),
    val saveTranslationChanges: Boolean = true,
    val searchQuery: String = "",
    val error: DataLoadError? = null,
    val outdatedSlugs: Set<String> = emptySet(),
    val updatingSlugs: Set<String> = emptySet(),
)

sealed interface TranslationEvent {
    object Refresh : TranslationEvent
    object RefreshQuiet : TranslationEvent
    data class Initialize(
        val initialSlugs: Set<String>,
        val saveTranslationChanges: Boolean
    ) : TranslationEvent

    data class ToggleGroup(val langCode: String) : TranslationEvent
    data class SelectionChanged(val translation: TranslModel, val isSelected: Boolean) :
        TranslationEvent

    data class Search(val query: String) : TranslationEvent
    data class DeleteTranslation(val slug: String) : TranslationEvent
    data class UpdateTranslation(val slug: String) : TranslationEvent
}

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()
    private val versionManager = TranslationVersionManager.get(context)

    init {
        TranslationDownloadManager.initialize(context)

        viewModelScope.launch {
            val initialSlugs = ReaderPreferences.getTranslations()
            _uiState.update { it.copy(selectedSlugs = initialSlugs) }

            loadTranslations()
        }

        viewModelScope.launch {
            versionManager.refreshOutdatedState()
        }

        viewModelScope.launch {
            versionManager.updateUiState.collect { state ->
                _uiState.update {
                    it.copy(outdatedSlugs = state.outdatedSlugs)
                }
            }
        }

        viewModelScope.launch {
            TranslationDownloadManager.observeDownloadsAsFlow().collect { (slug, status) ->
                val isUpdating = _uiState.value.updatingSlugs.contains(slug)
                if (!isUpdating) return@collect

                when (status) {
                    is ResourceDownloadStatus.Completed -> {
                        versionManager.markTranslationUpdated(slug)
                        _uiState.update {
                            it.copy(updatingSlugs = it.updatingSlugs - slug)
                        }
                        loadTranslations(silent = true)
                    }

                    is ResourceDownloadStatus.Failed,
                    is ResourceDownloadStatus.Cancelled -> {
                        _uiState.update {
                            it.copy(updatingSlugs = it.updatingSlugs - slug)
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    fun onEvent(event: TranslationEvent) {
        when (event) {
            is TranslationEvent.Refresh -> loadTranslations()
            is TranslationEvent.RefreshQuiet -> loadTranslations(silent = true)
            is TranslationEvent.Initialize -> _uiState.update {
                it.copy(
                    selectedSlugs = event.initialSlugs,
                    saveTranslationChanges = event.saveTranslationChanges
                )
            }

            is TranslationEvent.ToggleGroup -> toggleGroup(event.langCode)
            is TranslationEvent.SelectionChanged -> onSelectionChanged(
                event.translation,
                event.isSelected
            )

            is TranslationEvent.Search -> _uiState.update { it.copy(searchQuery = event.query) }
            is TranslationEvent.DeleteTranslation -> delete(event.slug)
            is TranslationEvent.UpdateTranslation -> updateTranslation(event.slug)
        }
    }


    private fun toggleGroup(langCode: String) {
        _uiState.update { state ->
            val updatedGroups = state.translationGroups.map { group ->
                if (group.langCode == langCode) {
                    group.copy(
                        isExpanded = !group.isExpanded
                    )
                } else {
                    group
                }
            }
            state.copy(translationGroups = updatedGroups)
        }
    }

    private fun onSelectionChanged(translation: TranslModel, isSelected: Boolean) {
        val state = _uiState.value
        val newSlugs = state.selectedSlugs.toMutableSet()
        val succeed = TranslUtils.resolveSelectionChange(
            application,
            newSlugs,
            translation,
            isSelected,
        )

        if (succeed && state.saveTranslationChanges) {
            viewModelScope.launch {
                ReaderPreferences.setTranslations(newSlugs)
            }
        }

        if (succeed) {
            val selectedSlugs = newSlugs.toSet()
            _uiState.update { current ->
                current.copy(
                    selectedSlugs = selectedSlugs,
                    translationGroups = current.translationGroups.map { group ->
                        group.copy(
                            translations = ArrayList(
                                group.translations.map { t ->
                                    t.apply { isChecked = selectedSlugs.contains(t.bookInfo.slug) }
                                }
                            )
                        )
                    }
                )
            }
        }
    }

    private fun delete(slug: String) {
        viewModelScope.launch {
            deleteTranslationNow(slug)
        }
    }

    private suspend fun deleteTranslationNow(slug: String) {
        if (TranslUtils.isPrebuilt(slug)) {
            return
        }

        var updatedSelectedSlugs: Set<String> = emptySet()
        DatabaseProvider.getTranslationRepository(application).let {
            it.delete(slug)
            SearchIndexScheduler.enqueueRemoveSlug(application.applicationContext, slug)

            _uiState.update { current ->
                val updatedGroups = current.translationGroups.map { group ->
                    val filtered = group.translations.filterNot { it.bookInfo.slug == slug }

                    group.copy(
                        translations = ArrayList(filtered)
                    )
                }.filterNot { it.translations.isEmpty() }

                updatedSelectedSlugs = current.selectedSlugs - slug
                current.copy(
                    translationGroups = updatedGroups,
                    selectedSlugs = updatedSelectedSlugs
                )
            }
        }

        viewModelScope.launch {
            ReaderPreferences.setTranslations(updatedSelectedSlugs)
        }
    }

    private fun updateTranslation(slug: String) {
        if (TranslUtils.isPrebuilt(slug)) return
        if (!_uiState.value.outdatedSlugs.contains(slug)) return
        if (!NetworkStateReceiver.canProceed(context.applicationContext)) return

        viewModelScope.launch {
            val remoteInfo = versionManager.getRemoteBookInfo(slug)
                ?: DatabaseProvider.getTranslationRepository(context.applicationContext).let { factory ->
                    factory.getBook(slug)
                }

            if (remoteInfo.downloadPath.isBlank()) return@launch

            _uiState.update {
                it.copy(updatingSlugs = it.updatingSlugs + slug)
            }
            TranslationDownloadManager.startDownload(context.applicationContext, remoteInfo)
        }
    }


    fun loadTranslations(
        silent: Boolean = false
    ) {
        _uiState.update { it.copy(isLoading = !silent, error = null) }

        viewModelScope.launch {
            val currentSlugs = _uiState.value.selectedSlugs

            try {
                val translationGroups = withContext(Dispatchers.IO) {
                    loadTranslationsFromDatabase(
                        _uiState.value.translationGroups,
                        currentSlugs
                    )
                }

                if (translationGroups.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            translationGroups = emptyList(),
                            error = DataLoadError.NoData
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            translationGroups = translationGroups,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        translationGroups = emptyList(),
                        error = DataLoadError.Failed
                    )
                }
            }
        }
    }

    private suspend fun loadTranslationsFromDatabase(
        oldGroups: List<TranslationGroupModel>,
        selectedSlugs: Set<String>
    ): List<TranslationGroupModel> {
        val fileUtils = FileUtils.newInstance(context)
        val translFactory = DatabaseProvider.getTranslationRepository(fileUtils.context)

        // map old groups expanded state for better UX
        val oldExpandedState = oldGroups.associate { it.langCode to it.isExpanded }

        val translationGroups = mutableListOf<TranslationGroupModel>()
        val languageAndInfo = mutableMapOf<String, MutableList<TranslationBookInfoModel>>()

        for (bookInfo in translFactory.getBooks().values) {
            val listOfLang = languageAndInfo.getOrPut(bookInfo.langCode) {
                mutableListOf()
            }
            listOfLang.add(bookInfo)
        }

        languageAndInfo.forEach { (langCode, listOfBooks) ->
            val groupModel = TranslationGroupModel(langCode)
            groupModel.langName = listOfBooks.firstOrNull()?.langName ?: langCode

            for (book in listOfBooks) {
                val model = TranslModel(book)
                model.isChecked = selectedSlugs.contains(book.slug)
                groupModel.translations.add(model)

                val wasExpanded = oldExpandedState[langCode] ?: false
                groupModel.isExpanded = groupModel.isExpanded || wasExpanded || model.isChecked
            }

            translationGroups.add(groupModel)
        }

        return translationGroups
    }
}
