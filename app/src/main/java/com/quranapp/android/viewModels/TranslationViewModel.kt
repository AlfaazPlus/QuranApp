package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.sharedPrefs.SPReader
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
    val error: DataLoadError? = null
)

sealed interface TranslationEvent {
    object Refresh : TranslationEvent
    data class Initialize(
        val initialSlugs: Set<String>,
        val saveTranslationChanges: Boolean
    ) : TranslationEvent

    data class ToggleGroup(val langCode: String) : TranslationEvent
    data class SelectionChanged(val translation: TranslModel, val isSelected: Boolean) :
        TranslationEvent

    data class Search(val query: String) : TranslationEvent
    data class DeleteTranslation(val slug: String) : TranslationEvent
}

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()

    init {
        val initialSlugs = SPReader.getSavedTranslations(context)
        _uiState.update { it.copy(selectedSlugs = initialSlugs) }
        loadTranslations()
    }

    fun onEvent(event: TranslationEvent) {
        when (event) {
            is TranslationEvent.Refresh -> loadTranslations()
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
            is TranslationEvent.DeleteTranslation -> deleteTranslation(event.slug)
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
            state.saveTranslationChanges
        )

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

    private fun deleteTranslation(slug: String) {
        QuranTranslationFactory(application).use {
            it.deleteTranslation(slug)

            _uiState.update { current ->
                val updatedGroups = current.translationGroups.map { group ->
                    val filtered = group.translations.filterNot { it.bookInfo.slug == slug }

                    group.copy(
                        translations = ArrayList(filtered)
                    )
                }.filterNot { it.translations.isEmpty() }

                current.copy(
                    translationGroups = updatedGroups,
                    selectedSlugs = current.selectedSlugs - slug
                )
            }
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

    private fun loadTranslationsFromDatabase(
        oldGroups: List<TranslationGroupModel>,
        selectedSlugs: Set<String>
    ): List<TranslationGroupModel> {
        val fileUtils = FileUtils.newInstance(context)
        val translFactory = QuranTranslationFactory(fileUtils.context)

        // map old groups expanded state for better UX
        val oldExpandedState = oldGroups.associate { it.langCode to it.isExpanded }

        return try {
            val translationGroups = mutableListOf<TranslationGroupModel>()

            val languageAndInfo =
                mutableMapOf<String, MutableList<TranslationBookInfoModel>>()

            for (bookInfo in translFactory.getAvailableTranslationBooksInfo().values) {
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

            translationGroups
        } finally {
            translFactory.close()
        }
    }
}