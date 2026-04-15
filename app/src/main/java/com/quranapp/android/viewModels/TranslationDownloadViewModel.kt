package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.managers.TranslationDownloadManager
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.LinkedList

data class TranslationDownloadUiState(
    val isLoading: Boolean = true,
    val groups: List<TranslationGroupModel> = emptyList(),
    val error: DataLoadError? = null,
    val downloadStates: Map<String, ResourceDownloadStatus> = emptyMap(),
    val isAnyDownloadCompleted: Boolean = false,
    val searchQuery: String = "",
)


sealed interface TranslationDownloadEvent {
    object Refresh : TranslationDownloadEvent
    data class ToggleGroup(val langCode: String) : TranslationDownloadEvent
    data class DownloadTranslation(val slug: String) : TranslationDownloadEvent
    data class CancelDownload(val slug: String) : TranslationDownloadEvent
    data class Search(val query: String) : TranslationDownloadEvent
}

sealed interface TranslationDownloadUiEvent {
    data class ShowMessage(val title: String, val message: String?) : TranslationDownloadUiEvent
}

class TranslationDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TranslationDownloadUiState())
    val uiState: StateFlow<TranslationDownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TranslationDownloadUiEvent>()
    val events = _events.asSharedFlow()

    private val context get() = getApplication<Application>().applicationContext

    init {
        TranslationDownloadManager.initialize(context)
        loadAvailableTranslations(force = SPAppActions.getFetchTranslationsForce(context))
        observeDownloadStates()
    }

    private fun observeDownloadStates() {
        viewModelScope.launch {
            TranslationDownloadManager.observeDownloadsAsFlow().collect { (key, status) ->
                updateDownloadStatus(key, status)
            }
        }
    }

    fun onEvent(event: TranslationDownloadEvent) {
        when (event) {
            is TranslationDownloadEvent.Refresh -> loadAvailableTranslations(force = true)
            is TranslationDownloadEvent.ToggleGroup -> toggleGroup(event.langCode)
            is TranslationDownloadEvent.DownloadTranslation -> downloadTranslation(event.slug)
            is TranslationDownloadEvent.CancelDownload -> cancelDownload(event.slug)
            is TranslationDownloadEvent.Search -> _uiState.update { it.copy(searchQuery = event.query) }
        }
    }

    private fun loadAvailableTranslations(force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(context)) {
            _uiState.update { it.copy(isLoading = false, error = DataLoadError.NoConnection) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val fileUtils = FileUtils.newInstance(context)
                val manifestFile = fileUtils.translsManifestFile

                val responseBody = RetrofitInstance.github.getAvailableTranslations()
                responseBody.string().let { data ->
                    fileUtils.createFile(manifestFile)
                    manifestFile.writeText(data)

                    val groups = parseAvailableTranslations(data)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groups = groups,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.saveError(e, "TranslationDownloadViewModel.loadAvailableTranslations")

                _uiState.update {
                    it.copy(isLoading = false, error = DataLoadError.Failed)
                }
            }
        }
    }

    private fun parseAvailableTranslations(data: String): List<TranslationGroupModel> {
        val translationGroups = LinkedList<TranslationGroupModel>()

        val obj = JsonHelper.json.parseToJsonElement(data).jsonObject
        obj["translations"]?.jsonObject?.let { translations ->

            val isAnyDownloadInProgress = TranslationDownloadManager.isAnyDownloading()

            for (langCode in translations.keys) {
                val translationsForLanguageCode = translations[langCode]?.jsonObject
                val slugs = translationsForLanguageCode?.keys ?: continue

                val groupModel = TranslationGroupModel(langCode)
                val translationItems = ArrayList<TranslModel>()

                for (slug in slugs) {
                    if (isTranslationDownloaded(slug)) continue

                    val model = readTranslInfo(
                        langCode,
                        slug,
                        translationsForLanguageCode[slug]!!.jsonObject
                    )

                    if (isAnyDownloadInProgress) {
                        model.isDownloadingDisabled = true
                    }

                    model.isDownloading = TranslationDownloadManager.isDownloading(slug)

                    groupModel.langName = model.bookInfo.langName
                    groupModel.isExpanded = groupModel.isExpanded || model.isDownloading

                    translationItems.add(model)
                }

                // If no translation was added in this language category, skip
                if (translationItems.isEmpty()) continue

                groupModel.translations = translationItems
                translationGroups.add(groupModel)
            }
        }

        SPAppActions.setFetchTranslationsForce(context, false)
        return translationGroups
    }

    private fun readTranslInfo(
        langCode: String,
        slug: String,
        translObject: JsonObject
    ): TranslModel {
        val bookInfo = TranslationBookInfoModel(slug)
        bookInfo.langCode = langCode
        bookInfo.bookName = translObject["book"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.authorName = translObject["author"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.displayName = translObject["displayName"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.langName = translObject["langName"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.lastUpdated = translObject["lastUpdated"]?.jsonPrimitive?.longOrNull ?: -1
        bookInfo.downloadPath = translObject["downloadPath"]?.jsonPrimitive?.contentOrNull ?: ""
        return TranslModel(bookInfo)
    }

    private fun toggleGroup(langCode: String) {
        _uiState.update { state ->
            val updatedGroups = state.groups.map { group ->
                if (group.langCode == langCode) {
                    TranslationGroupModel(group.langCode).apply {
                        langName = group.langName
                        translations = group.translations
                        isExpanded = !group.isExpanded
                    }
                } else {
                    group
                }
            }
            state.copy(groups = updatedGroups)
        }
    }

    private fun downloadTranslation(slug: String) {
        val model = findTranslationBySlug(slug) ?: return
        val bookInfo = model.bookInfo

        if (TranslationDownloadManager.isDownloading(bookInfo.slug)) {
            updateDownloadStatus(bookInfo.slug, ResourceDownloadStatus.Started)
            return
        }

        if (isTranslationDownloaded(bookInfo.slug)) {
            updateDownloadStatus(bookInfo.slug, ResourceDownloadStatus.Completed)
            return
        }

        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        updateDownloadStatus(bookInfo.slug, ResourceDownloadStatus.Started)
        TranslationDownloadManager.startDownload(context, bookInfo)
    }

    private fun cancelDownload(key: String) {
        TranslationDownloadManager.stopDownload(context, key)
        updateDownloadStatus(key, ResourceDownloadStatus.Cancelled)
    }


    private fun updateDownloadStatus(slug: String, status: ResourceDownloadStatus) {
        val bookInfo = findTranslationBySlug(slug)?.bookInfo ?: return

        _uiState.update { state ->
            var newState = state
            val newDownloadStates = state.downloadStates.toMutableMap()
            val newGroups = state.groups.toMutableList()

            // Remove completed/cancelled states, keep failed for retry UI
            when (status) {
                is ResourceDownloadStatus.Failed -> {
                    viewModelScope.launch {
                        _events.emit(
                            TranslationDownloadUiEvent.ShowMessage(
                                title = context.getString(R.string.strTitleFailed),
                                message = context.getString(
                                    R.string.strMsgTranslFailedToDownload,
                                    bookInfo.bookName
                                ) +
                                        " " + context.getString(R.string.strMsgTryLater)
                            )
                        )
                    }
                    newDownloadStates.remove(slug)
                }

                is ResourceDownloadStatus.Completed -> {
                    viewModelScope.launch {
                        _events.emit(
                            TranslationDownloadUiEvent.ShowMessage(
                                title = context.getString(R.string.strTitleSuccess),
                                message = context.getString(
                                    R.string.strMsgTranslDownloaded,
                                    bookInfo.bookName
                                )
                            )
                        )
                    }

                    newDownloadStates.remove(slug)

                    // remove from available list
                    for (group in newGroups) {
                        val iterator = group.translations.iterator()
                        while (iterator.hasNext()) {
                            val t = iterator.next()
                            if (t.bookInfo.slug == slug) {
                                iterator.remove()
                                break
                            }
                        }
                    }

                    newState = newState.copy(isAnyDownloadCompleted = true)
                }

                is ResourceDownloadStatus.Cancelled -> {
                    newDownloadStates.remove(slug)
                }

                else -> {
                    newDownloadStates[slug] = status
                }
            }

            newState.copy(
                downloadStates = newDownloadStates,
                groups = newGroups.filterNot { it.translations.isEmpty() }
            )
        }
    }


    private fun findTranslationBySlug(slug: String): TranslModel? {
        for (group in _uiState.value.groups) {
            for (t in group.translations) {
                if (t.bookInfo.slug == slug) {
                    return t
                }
            }
        }

        return null
    }


    private fun isTranslationDownloaded(slug: String): Boolean {
        var translFactory = QuranTranslationFactory(application)

        try {
            return translFactory.isTranslationDownloaded(slug) == true
        } finally {
            translFactory.close()
        }
    }
}
