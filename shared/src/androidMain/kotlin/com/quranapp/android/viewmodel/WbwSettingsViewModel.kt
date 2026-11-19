package com.quranapp.android.viewmodel

import androidx.lifecycle.viewModelScope
import com.quranapp.android.AndroidAppContext
import com.quranapp.android.api.models.wbw.WbwLanguageInfo
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.models.DataLoadError
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.reader.wbw.WbwDownloadManager
import com.quranapp.android.utils.wbw.WbwManager
import com.quranapp.android.utils.wbw.WbwVersionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WbwUiModel(
    val info: WbwLanguageInfo,
    val isDownloaded: Boolean,
    val isUpdateAvailable: Boolean,
)

data class WbwSettingsUiState(
    val isLoading: Boolean = true,
    val error: DataLoadError? = null,
    val selectedWbwId: String? = null,
    val rows: List<WbwUiModel> = emptyList(),
    val downloadStates: Map<String, ResourceDownloadStatus> = emptyMap(),
    val hasAnyUpdates: Boolean = false,
)

class WbwSettingsViewModel() : AppViewModel() {
    private val context get() = AndroidAppContext.get()
    private val db get() = DatabaseProvider.getExternalQuranDatabase()
    private val versionManager = WbwVersionManager.get()

    private val _uiState = MutableStateFlow(WbwSettingsUiState())
    val uiState: StateFlow<WbwSettingsUiState> = _uiState.asStateFlow()

    init {
        WbwDownloadManager.initialize(context)
        observeSelection()
        observeDownloads()
        observeVersionState()
        load(force = false)
        viewModelScope.launch {
            versionManager.refreshOutdatedState()
        }
    }

    private fun observeSelection() {
        viewModelScope.launch {
            ReaderPreferences.wbwIdFlow().collect { selected ->
                _uiState.update { it.copy(selectedWbwId = selected) }
            }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            WbwDownloadManager.observeDownloadsAsFlow().collect { (id, status) ->
                _uiState.update { state ->
                    val next = state.downloadStates.toMutableMap()
                    when (status) {
                        is ResourceDownloadStatus.Completed,
                        is ResourceDownloadStatus.Cancelled -> next.remove(id)

                        else -> next[id] = status
                    }
                    state.copy(downloadStates = next)
                }

                if (status is ResourceDownloadStatus.Completed) {
                    versionManager.markResourceUpdated(id)
                    refreshRows()
                }
            }
        }
    }

    private fun observeVersionState() {
        viewModelScope.launch {
            versionManager.updateUiState.collect { state ->
                _uiState.update {
                    it.copy(hasAnyUpdates = state.hasOutdatedDownloads)
                }
                refreshRows()
            }
        }
    }

    fun load(force: Boolean) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val manifest = WbwManager.getAvailable(forceRefresh = force)
            versionManager.refreshOutdatedState()

            if (manifest == null) {
                _uiState.update { it.copy(isLoading = false, error = DataLoadError.Failed) }
                return@launch
            }

            val rows = buildRows(manifest.wbw)
            val selected = resolveSelectedId(rows)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (rows.isEmpty()) DataLoadError.NoData else null,
                    rows = rows,
                    selectedWbwId = selected,
                )
            }
        }
    }

    private suspend fun refreshRows() {
        val rows = _uiState.value.rows
        if (rows.isEmpty()) return
        val refreshed = buildRows(rows.map { it.info })
        val selected = resolveSelectedId(refreshed)

        _uiState.update {
            it.copy(
                rows = refreshed,
                selectedWbwId = selected,
                error = if (refreshed.isEmpty()) DataLoadError.NoData else null,
            )
        }
    }

    private suspend fun buildRows(
        languages: List<WbwLanguageInfo>
    ): List<WbwUiModel> = withContext(Dispatchers.IO) {
        if (languages.isEmpty()) return@withContext emptyList()

        val wbwIds = languages.map { it.id }.distinct()
        val downloadedIds = db
            .wbwDao()
            .getDownloadedWbwIds(wbwIds)
            .toSet()

        return@withContext languages
            .sortedWith(
                compareBy<WbwLanguageInfo> {
                    if (it.langCode.equals("en", ignoreCase = true)) 0 else 1
                }.thenBy { it.langName }.thenBy { it.langCode }
            )
            .map { info ->
                val isDownloaded = downloadedIds.contains(info.id)
                val isUpdateAvailable = isDownloaded && versionManager.needsUpdate(info.id)
                WbwUiModel(
                    info = info,
                    isDownloaded = isDownloaded,
                    isUpdateAvailable = isUpdateAvailable
                )
            }
    }

    private suspend fun resolveSelectedId(rows: List<WbwUiModel>): String? {
        val downloadedIds = rows.filter { it.isDownloaded }.map { it.info.id }.toSet()
        val preferred = ReaderPreferences.getWbwId()

        return validateWbwId(preferred, downloadedIds)
    }

    private fun validateWbwId(
        id: String?,
        availableIds: Set<String>,
        fallback: String? = null,
    ): String? {
        val normalizedId = id?.takeIf { it.isNotBlank() }

        if (normalizedId != null && availableIds.contains(normalizedId)) {
            return normalizedId
        }

        return null
    }

    fun selectLanguage(id: String) {
        val selectedRow = _uiState.value.rows.firstOrNull { it.info.id == id } ?: return
        if (!selectedRow.isDownloaded) return

        viewModelScope.launch {
            ReaderPreferences.setWbwId(id)
        }
    }

    fun startDownload(id: String) {
        val info = _uiState.value.rows.firstOrNull { it.info.id == id }?.info ?: return
        WbwDownloadManager.startDownload(context, info)
        _uiState.update {
            it.copy(downloadStates = it.downloadStates + (id to ResourceDownloadStatus.Started))
        }
    }

    fun cancelDownload(id: String) {
        WbwDownloadManager.stopDownload(context, id)
        _uiState.update {
            it.copy(downloadStates = it.downloadStates - id)
        }
    }

    fun deleteWbwData(id: String) {
        viewModelScope.launch {
            db.wbwDao().deleteByWbwId(id)
            versionManager.refreshOutdatedState()
            refreshRows()
        }
    }
}
