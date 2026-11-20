package com.quranapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.extensions.canProceed
import com.quranapp.android.platform.Platform
import com.quranapp.android.task.TaskState
import com.quranapp.android.utils.managers.AtlasDownloadManager
import com.quranapp.android.utils.managers.ScriptFontsDownloadManager
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.isPrebuiltAtlas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScriptsUiState(
    val scripts: Map<String, List<QuranScriptVariant>> = QuranScriptUtils.availableScripts(),
    val downloadStates: Map<String, TaskState> = emptyMap(),
)

sealed interface ScriptEvent {
    data class DownloadScript(val key: String) : ScriptEvent
    data class DownloadAtlas(val context: Context, val key: String, val densityLevel: Int) :
        ScriptEvent

    data class CancelDownload(val context: Context, val key: String) : ScriptEvent
}

class ScriptsViewModel() : AppViewModel() {
    private val _uiState = MutableStateFlow(ScriptsUiState())
    val uiState: StateFlow<ScriptsUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        ScriptFontsDownloadManager.initialize()
        AtlasDownloadManager.initialize()
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
        if (script.isPrebuiltAtlas()) return@withContext true

        DatabaseProvider.getExternalQuranDatabase()
            .atlasWordShapeDao()
            .getBundleByKey(script) != null
    }

    fun onEvent(event: ScriptEvent) {
        when (event) {
            is ScriptEvent.DownloadScript -> downloadScript(event.key)
            is ScriptEvent.DownloadAtlas -> downloadAtlas(
                event.context,
                event.key,
                event.densityLevel
            )

            is ScriptEvent.CancelDownload -> cancelDownload(event.context, event.key)
        }
    }

    private fun downloadScript(key: String) {
        viewModelScope.launch {
            if (!Platform.networkMonitor.canProceed()) return@launch

            ScriptFontsDownloadManager.startDownload(key)
            updateDownloadStatus(key, TaskState.Enqueued)
        }
    }

    private fun downloadAtlas(context: Context, key: String, densityLevel: Int) {
        viewModelScope.launch {
            if (!Platform.networkMonitor.canProceed()) return@launch

            AtlasDownloadManager.startDownload(key, densityLevel)
            updateDownloadStatus(key, TaskState.Enqueued)
        }
    }

    private fun cancelDownload(context: Context, key: String) {
        ScriptFontsDownloadManager.stopDownload(key)
        AtlasDownloadManager.stopDownload(key)
        updateDownloadStatus(key, TaskState.Cancelled)
    }

    private fun updateDownloadStatus(key: String, status: TaskState) {
        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()

            when (status) {
                is TaskState.Succeeded -> {
                    newDownloadStates.remove(key)
                }

                TaskState.Cancelled -> {
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
