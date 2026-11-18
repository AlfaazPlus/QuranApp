package com.quranapp.android.utils.mediaplayer

import android.content.Context
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.app.AppFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RecitationUpdateUiState(
    val outdatedReciters: Set<String> = emptySet(),
) {
    val hasOutdatedDownloads: Boolean get() = outdatedReciters.isNotEmpty()
}

data class ManifestVersionSnapshot(
    val reciterId: String,
    val audioVersion: Int,
    val timingVersion: Int,
    val urlTemplate: String,
)

class RecitationVersionManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val modelManager = RecitationModelManager
    private val reconcileMutex = Mutex()

    private val _updateUiState = MutableStateFlow(RecitationUpdateUiState())
    val updateUiState: StateFlow<RecitationUpdateUiState> = _updateUiState.asStateFlow()

    suspend fun reconcileAfterManifestRefresh() = withContext(Dispatchers.IO) {
        reconcileMutex.withLock {
            runCatching {
                val manifest = loadManifestSnapshots()
                if (manifest.isEmpty()) {
                    _updateUiState.value = RecitationUpdateUiState(emptySet())
                    return@runCatching
                }

                val stored = RecitationVersionStore.loadAll()
                val outdated = mutableSetOf<String>()
                var mutated = false

                for ((reciterId, remote) in manifest) {
                    val existing = stored[reciterId]
                    val hasDownloaded = hasDownloadedAudio(reciterId)

                    if (existing == null) {
                        stored[reciterId] = ReciterStoredVersions(
                            audio = remote.audioVersion,
                            timing = remote.timingVersion,
                        )
                        mutated = true
                        continue
                    }

                    var next = existing

                    if (existing.timing < remote.timingVersion) {
                        AppFiles.delete(modelManager.getRecitationTimingFile(reciterId))
                        RecitationStreamCache.clearForReciter(appContext, remote.urlTemplate)
                        next = next.copy(timing = remote.timingVersion)
                    }

                    if (existing.audio < remote.audioVersion) {
                        RecitationStreamCache.clearForReciter(appContext, remote.urlTemplate)

                        if (hasDownloaded) {
                            outdated.add(reciterId)
                        } else {
                            next = next.copy(audio = remote.audioVersion)
                        }
                    }

                    if (next != existing) {
                        stored[reciterId] = next
                        mutated = true
                    }
                }

                if (mutated) {
                    RecitationVersionStore.saveAll(stored)
                }

                _updateUiState.value = RecitationUpdateUiState(outdated)
            }.onFailure {
                Logger.saveError(it, "RecitationVersionManager.reconcileAfterManifestRefresh")
            }
        }
    }

    suspend fun refreshOutdatedState() = withContext(Dispatchers.IO) {
        reconcileMutex.withLock {
            runCatching {
                val manifest = loadManifestSnapshots()
                if (manifest.isEmpty()) {
                    _updateUiState.value = RecitationUpdateUiState(emptySet())
                    return@runCatching
                }

                val stored = RecitationVersionStore.loadAll()
                val outdated = mutableSetOf<String>()
                var mutated = false

                for ((reciterId, remote) in manifest) {
                    val existing = stored[reciterId]
                    val hasDownloaded = hasDownloadedAudio(reciterId)

                    if (existing == null) {
                        stored[reciterId] = ReciterStoredVersions(
                            audio = remote.audioVersion,
                            timing = remote.timingVersion,
                        )
                        mutated = true
                        continue
                    }

                    if (existing.audio < remote.audioVersion && hasDownloaded) {
                        outdated.add(reciterId)
                    } else if (existing.audio < remote.audioVersion && !hasDownloaded) {
                        stored[reciterId] = existing.copy(audio = remote.audioVersion)
                        mutated = true
                    }

                    if (existing.timing < remote.timingVersion && !hasDownloaded) {
                        stored[reciterId] = stored[reciterId]!!.copy(timing = remote.timingVersion)
                        mutated = true
                    }
                }

                if (mutated) {
                    RecitationVersionStore.saveAll(stored)
                }

                _updateUiState.value = RecitationUpdateUiState(outdated)
            }.onFailure {
                Logger.saveError(it, "RecitationVersionManager.refreshOutdatedState")
            }
        }
    }

    suspend fun markAudioUpdated(reciterId: String) = withContext(Dispatchers.IO) {
        reconcileMutex.withLock {
            runCatching {
                val manifest = loadManifestSnapshots()
                val remote = manifest[reciterId] ?: return@runCatching
                val stored = RecitationVersionStore.loadAll()
                val existing = stored[reciterId]
                val next = ReciterStoredVersions(
                    audio = remote.audioVersion,
                    timing = maxOf(existing?.timing ?: 0, remote.timingVersion),
                )
                stored[reciterId] = next
                RecitationVersionStore.saveAll(stored)
                _updateUiState.value = _updateUiState.value.copy(
                    outdatedReciters = _updateUiState.value.outdatedReciters - reciterId,
                )
            }.onFailure {
                Logger.saveError(it, "RecitationVersionManager.markAudioUpdated")
            }
        }
    }

    fun needsAudioUpdate(reciterId: String): Boolean {
        return _updateUiState.value.outdatedReciters.contains(reciterId)
    }

    private suspend fun loadManifestSnapshots(): Map<String, ManifestVersionSnapshot> {
        val snapshots = LinkedHashMap<String, ManifestVersionSnapshot>()

        modelManager.getAllQuranModel()?.reciters.orEmpty().forEach { model ->
            snapshots[model.id] = model.toManifestSnapshot()
        }

        modelManager.getAllTranslationModel()?.reciters.orEmpty().forEach { model ->
            snapshots[model.id] = model.toManifestSnapshot()
        }

        return snapshots
    }

    private fun RecitationModelBase.toManifestSnapshot(): ManifestVersionSnapshot {
        return ManifestVersionSnapshot(
            reciterId = id,
            audioVersion = audioVersion ?: 1,
            timingVersion = timingVersion ?: 0,
            urlTemplate = urlTemplate,
        )
    }

    private fun hasDownloadedAudio(reciterId: String): Boolean {
        for (chapterNo in 1..114) {
            val file = modelManager.getRecitationAudioFile(reciterId, chapterNo)
            if (AppFiles.size(file) > 0L) {
                return true
            }
        }
        return false
    }

    companion object {
        @Volatile
        private var instance: RecitationVersionManager? = null

        fun get(context: Context): RecitationVersionManager {
            return instance ?: synchronized(this) {
                instance ?: RecitationVersionManager(context).also { instance = it }
            }
        }
    }
}
