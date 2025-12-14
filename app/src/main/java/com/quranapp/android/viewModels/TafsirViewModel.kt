package com.quranapp.android.viewModels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.components.tafsir.TafsirGroupModel
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.utils.maangers.ResourceDownloadStatus
import com.quranapp.android.utils.maangers.TafsirDownloadManager
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList

/**
 * Represents the download state for a tafsir
 */
sealed interface TafsirDownloadState {
    object Idle : TafsirDownloadState
    object Started : TafsirDownloadState
    data class Downloading(val progress: Int) : TafsirDownloadState
    object Completed : TafsirDownloadState
    data class Failed(val error: String?) : TafsirDownloadState
    object Cancelled : TafsirDownloadState
}

data class TafsirUiState(
    val isLoading: Boolean = true,
    val tafsirGroups: List<TafsirGroupModel> = emptyList(),
    val selectedTafsirKey: String? = null,
    val error: DataLoadError? = null,
    val downloadStates: Map<String, TafsirDownloadState> = emptyMap(),
    val downloadedTafsirKeys: Set<String> = emptySet(),
    val tafsirSelectionChanged: Boolean = false
)


sealed interface TafsirEvent {
    object Refresh : TafsirEvent
    data class ToggleGroup(val langCode: String) : TafsirEvent
    data class SelectTafsir(val key: String) : TafsirEvent
    data class DownloadTafsir(val key: String) : TafsirEvent
    data class CancelDownload(val key: String) : TafsirEvent
    data class DeleteTafsir(val key: String) : TafsirEvent
    object ClearSelectionChanged : TafsirEvent
}

class TafsirViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TafsirUiState())
    val uiState: StateFlow<TafsirUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()
    private var initialTafsirKey: String? = null

    init {
        initialTafsirKey = SPReader.getSavedTafsirKey(context)
        TafsirDownloadManager.initialize(context)
        loadDownloadedTafsirKeys()
        loadTafsirs(force = SPAppActions.getFetchTafsirsForce(context))
        observeDownloadStates()
    }

    private fun loadDownloadedTafsirKeys() {
        viewModelScope.launch {
            val downloadedKeys = withContext(Dispatchers.IO) {
                QuranTafsirDBHelper(context).use {
                    it.getDownloadedTafsirKeys()
                }
            }
            _uiState.update { it.copy(downloadedTafsirKeys = downloadedKeys) }
        }
    }

    private fun observeDownloadStates() {
        viewModelScope.launch {
            TafsirDownloadManager.observeDownloadsAsFlow().collect { (key, status) ->
                updateDownloadStatus(key, status)
            }
        }
    }

    fun onEvent(event: TafsirEvent) {
        when (event) {
            is TafsirEvent.Refresh -> loadTafsirs(force = true)
            is TafsirEvent.ToggleGroup -> toggleGroup(event.langCode)
            is TafsirEvent.SelectTafsir -> selectTafsir(event.key)
            is TafsirEvent.DownloadTafsir -> downloadTafsir(event.key)
            is TafsirEvent.CancelDownload -> cancelDownload(event.key)
            is TafsirEvent.DeleteTafsir -> deleteTafsir(event.key)
            is TafsirEvent.ClearSelectionChanged -> clearSelectionChanged()
        }
    }

    fun hasTafsirSelectionChanged(): Boolean {
        return SPReader.getSavedTafsirKey(context) != initialTafsirKey
    }

    private fun loadTafsirs(force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(context)) {
            _uiState.update { it.copy(isLoading = false, error = DataLoadError.NoConnection) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            TafsirManager.prepare(context, force) {
                val models = TafsirManager.getModels()

                if (!models.isNullOrEmpty()) {
                    val savedKey = SPReader.getSavedTafsirKey(context)
                    val groups = parseAvailableTafsirs(models)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tafsirGroups = groups,
                            selectedTafsirKey = savedKey,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = DataLoadError.NoData)
                    }
                }
            }
        }
    }

    private fun parseAvailableTafsirs(tafsirs: Map<String, List<TafsirInfoModel>>): List<TafsirGroupModel> {
        val savedTafsirKey = SPReader.getSavedTafsirKey(context)
        val tafsirGroups = LinkedList<TafsirGroupModel>()

        for (langCode in tafsirs.keys) {
            val groupModel = TafsirGroupModel(langCode)
            val tafsirModels = tafsirs[langCode] ?: continue
            if (tafsirModels.isEmpty()) continue

            var groupHasItemSelected = false

            for (model in tafsirModels) {
                model.isChecked = model.key == savedTafsirKey
                if (model.isChecked) {
                    groupHasItemSelected = true
                }
            }

            groupModel.tafsirs = tafsirModels
            groupModel.langName = tafsirModels[0].langName
            groupModel.isExpanded = groupHasItemSelected

            tafsirGroups.add(groupModel)
        }

        return tafsirGroups
    }

    private fun toggleGroup(langCode: String) {
        _uiState.update { state ->
            val updatedGroups = state.tafsirGroups.map { group ->
                if (group.langCode == langCode) {
                    TafsirGroupModel(group.langCode).apply {
                        langName = group.langName
                        tafsirs = group.tafsirs
                        isExpanded = !group.isExpanded
                    }
                } else {
                    group
                }
            }
            state.copy(tafsirGroups = updatedGroups)
        }
    }

    private fun selectTafsir(key: String) {
        SPReader.setSavedTafsirKey(context, key)

        _uiState.update { state ->
            val updatedGroups = state.tafsirGroups.map { group ->
                TafsirGroupModel(group.langCode).apply {
                    langName = group.langName
                    isExpanded = group.isExpanded
                    tafsirs = group.tafsirs.map { tafsir ->
                        tafsir.apply { isChecked = this.key == key }
                    }
                }
            }
            state.copy(
                tafsirGroups = updatedGroups,
                selectedTafsirKey = key,
                tafsirSelectionChanged = key != initialTafsirKey
            )
        }
    }

    private fun downloadTafsir(key: String) {
        val tafsirInfo = findTafsirByKey(key) ?: return

        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            MessageUtils.showRemovableToast(
                context,
                R.string.strMsgNoInternet,
                Toast.LENGTH_LONG
            )
            return
        }

        TafsirDownloadManager.startDownload(context, tafsirInfo)

        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()
            newDownloadStates[key] = TafsirDownloadState.Started
            state.copy(downloadStates = newDownloadStates)
        }
    }

    private fun cancelDownload(key: String) {
        TafsirDownloadManager.stopDownload(context, key)

        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()
            newDownloadStates.remove(key)
            state.copy(downloadStates = newDownloadStates)
        }
    }

    private fun deleteTafsir(key: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                QuranTafsirDBHelper(context).use {
                    it.deleteTafsirData(key)
                }
            }

            // Update the downloaded keys set
            _uiState.update { state ->
                val newDownloadedKeys = state.downloadedTafsirKeys.toMutableSet()
                newDownloadedKeys.remove(key)
                state.copy(downloadedTafsirKeys = newDownloadedKeys)
            }
        }
    }

    private fun updateDownloadStatus(key: String, status: ResourceDownloadStatus) {
        val downloadState = when (status) {
            is ResourceDownloadStatus.Started -> TafsirDownloadState.Started
            is ResourceDownloadStatus.InProgress -> TafsirDownloadState.Downloading(status.progress)
            is ResourceDownloadStatus.Completed -> TafsirDownloadState.Completed
            is ResourceDownloadStatus.Failed -> TafsirDownloadState.Failed(status.error)
            is ResourceDownloadStatus.Cancelled -> TafsirDownloadState.Cancelled
        }

        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()
            var newDownloadedKeys = state.downloadedTafsirKeys

            // Remove completed/cancelled states, keep failed for retry UI
            when (downloadState) {
                is TafsirDownloadState.Completed -> {
                    newDownloadStates.remove(key)
                    // Add to downloaded keys
                    newDownloadedKeys = state.downloadedTafsirKeys + key
                }

                is TafsirDownloadState.Cancelled -> {
                    newDownloadStates.remove(key)
                }

                else -> {
                    newDownloadStates[key] = downloadState
                }
            }

            state.copy(
                downloadStates = newDownloadStates,
                downloadedTafsirKeys = newDownloadedKeys
            )
        }
    }

    private fun findTafsirByKey(key: String): TafsirInfoModel? {
        for (group in _uiState.value.tafsirGroups) {
            for (tafsir in group.tafsirs) {
                if (tafsir.key == key) {
                    return tafsir
                }
            }
        }
        return null
    }

    private fun clearSelectionChanged() {
        _uiState.update { it.copy(tafsirSelectionChanged = false) }
    }
}
