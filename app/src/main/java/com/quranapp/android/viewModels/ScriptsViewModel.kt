package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.reader.atlas.AtlasDownloadManager
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.managers.ScriptFontsDownloadManager
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScriptsUiState(
    val scripts: Map<String, List<QuranScriptVariant>> = QuranScriptUtils.availableScripts(),
    val downloadStates: Map<String, ResourceDownloadStatus> = emptyMap(),
)


sealed interface ScriptEvent {
    data class DownloadScript(val key: String) : ScriptEvent
    data class DownloadAtlas(val key: String, val densityLevel: Int) : ScriptEvent
    data class CancelDownload(val key: String) : ScriptEvent
}


class ScriptsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ScriptsUiState())
    val uiState: StateFlow<ScriptsUiState> = _uiState.asStateFlow()

    private val context get() = application

    init {
        ScriptFontsDownloadManager.initialize(context)
        AtlasDownloadManager.initialize(context)
        observeDownloadStates()
    }

    private fun observeDownloadStates() {
        viewModelScope.launch {
            ScriptFontsDownloadManager.observeDownloadsAsFlow().collect { (key, status) ->
                updateDownloadStatus(key, status)
            }
        }
        viewModelScope.launch {
            AtlasDownloadManager.observeDownloadsAsFlow().collect { (key, status) ->
                updateDownloadStatus(key, status)
            }
        }
    }

    suspend fun isAtlasInstalled(script: String): Boolean = withContext(Dispatchers.IO) {
        DatabaseProvider.getExternalQuranDatabase(context)
            .atlasWordShapeDao()
            .getBundleByKey(script) != null
    }

    fun onEvent(event: ScriptEvent) {
        when (event) {
            is ScriptEvent.DownloadScript -> downloadScript(event.key)
            is ScriptEvent.DownloadAtlas -> downloadAtlas(event.key, event.densityLevel)
            is ScriptEvent.CancelDownload -> cancelDownload(event.key)
        }
    }

    private fun downloadScript(key: String) {
        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        ScriptFontsDownloadManager.startDownload(context, key)
        updateDownloadStatus(key, ResourceDownloadStatus.Started)
    }

    private fun downloadAtlas(key: String, densityLevel: Int) {
        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        AtlasDownloadManager.startDownload(context, key, densityLevel)
        updateDownloadStatus(key, ResourceDownloadStatus.Started)
    }

    private fun cancelDownload(key: String) {
        ScriptFontsDownloadManager.stopDownload(context, key)
        AtlasDownloadManager.stopDownload(context, key)
        updateDownloadStatus(key, ResourceDownloadStatus.Cancelled)
    }

    private fun updateDownloadStatus(key: String, status: ResourceDownloadStatus) {
        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()

            when (status) {
                is ResourceDownloadStatus.Completed -> {
                    newDownloadStates.remove(key)
                }

                is ResourceDownloadStatus.Cancelled -> {
                    newDownloadStates.remove(key)
                }

                else -> {
                    newDownloadStates[key] = status
                }
            }

            state.copy(
                downloadStates = newDownloadStates,
            )
        }
    }
}
