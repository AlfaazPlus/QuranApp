package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.api.models.wbw.WbwLanguageInfo
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.managers.WbwDownloadManager
import com.quranapp.android.utils.reader.wbw.WbwManager
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
)

class WbwSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getExternalQuranDatabase(context)
    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(WbwSettingsUiState())
    val uiState: StateFlow<WbwSettingsUiState> = _uiState.asStateFlow()

    init {
        WbwDownloadManager.initialize(context)
        observeSelection()
        observeDownloads()
        load(force = false)
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
                    refreshRows()
                }
            }
        }
    }

    fun load(force: Boolean) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val manifest = WbwManager.getAvailable(context, forceRefresh = force)
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
                val localVersion = WbwManager.getResourceVersion(context, info.id)
                val isUpdateAvailable = isDownloaded && info.version > localVersion
                WbwUiModel(
                    info = info,
                    isDownloaded = isDownloaded,
                    isUpdateAvailable = isUpdateAvailable
                )
            }
    }

    private fun resolveSelectedId(rows: List<WbwUiModel>): String? {
        val availableIds = rows.map { it.info.id }.toSet()
        val downloadedIds = rows.filter { it.isDownloaded }.map { it.info.id }.toSet()
        val preferred = ReaderPreferences.getWbwId()
        val preferredByLocaleDownloaded = rows.findLocalePreferredId()

        return if (downloadedIds.isNotEmpty()) {
            ReaderPreferences.validateWbwId(preferred, downloadedIds, preferredByLocaleDownloaded)
        } else {
            ReaderPreferences.validateWbwId(preferred, availableIds, null)
        }
    }

    private fun List<WbwUiModel>.findLocalePreferredId(): String? {
        if (isEmpty()) return null

        val candidates = appFallbackLanguageCodes().toList()
        for (candidate in candidates) {
            val normalized = candidate.lowercase()
            val match = firstOrNull { row ->
                (row.isDownloaded) &&
                        row.info.langCode.lowercase() == normalized
            } ?: firstOrNull { row ->
                (row.isDownloaded) &&
                        normalized.startsWith("${row.info.langCode.lowercase()}-")
            }

            if (match != null) {
                return match.info.id
            }
        }

        return firstOrNull { it.isDownloaded }?.info?.id
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
            refreshRows()
        }
    }
}
