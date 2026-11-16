package com.quranapp.android.utils.reader.wbw

import android.content.Context
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class WbwUpdateUiState(
    val outdatedLanguageIds: Set<String> = emptySet(),
) {
    val hasOutdatedDownloads: Boolean get() = outdatedLanguageIds.isNotEmpty()
}

class WbwVersionManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    private val _updateUiState = MutableStateFlow(WbwUpdateUiState())
    val updateUiState: StateFlow<WbwUpdateUiState> = _updateUiState.asStateFlow()

    suspend fun reconcileAfterManifestRefresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val manifest = WbwManager.getAvailable(appContext, forceRefresh = false)
                    ?: return@runCatching
                val languages = manifest.wbw
                if (languages.isEmpty()) {
                    _updateUiState.value = WbwUpdateUiState(emptySet())
                    return@runCatching
                }

                val wbwIds = languages.map { it.id }.distinct()
                val downloadedIds = DatabaseProvider.getExternalQuranDatabase(appContext)
                    .wbwDao()
                    .getDownloadedWbwIds(wbwIds)
                    .toSet()

                val outdated = mutableSetOf<String>()
                for (info in languages) {
                    if (!downloadedIds.contains(info.id)) continue
                    val localVersion = WbwManager.getResourceVersion(appContext, info.id)
                    if (info.version > localVersion) {
                        outdated.add(info.id)
                    } else {
                        WbwManager.markResourceVersion(appContext, info.id, info.version)
                    }
                }
                _updateUiState.value = WbwUpdateUiState(outdated)
            }.onFailure {
                Log.saveError(it, "WbwVersionManager.reconcileAfterManifestRefresh")
            }
        }
    }

    suspend fun refreshOutdatedState() = reconcileAfterManifestRefresh()

    suspend fun markResourceUpdated(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val manifest = WbwManager.getAvailable(appContext, forceRefresh = false)
                    ?: return@runCatching
                val info = manifest.wbw.firstOrNull { it.id == id } ?: return@runCatching
                WbwManager.markResourceVersion(appContext, id, info.version)
                _updateUiState.value = _updateUiState.value.copy(
                    outdatedLanguageIds = _updateUiState.value.outdatedLanguageIds - id,
                )
            }.onFailure {
                Log.saveError(it, "WbwVersionManager.markResourceUpdated")
            }
        }
    }

    fun needsUpdate(id: String): Boolean {
        return _updateUiState.value.outdatedLanguageIds.contains(id)
    }

    companion object {
        @Volatile
        private var instance: WbwVersionManager? = null

        fun get(context: Context): WbwVersionManager {
            return instance ?: synchronized(this) {
                instance ?: WbwVersionManager(context).also { instance = it }
            }
        }
    }
}
