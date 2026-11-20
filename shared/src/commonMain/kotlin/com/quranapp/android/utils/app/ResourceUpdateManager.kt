package com.quranapp.android.utils.app

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.ResourcesVersions
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.RecitationVersionManager
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.share.VerseTemplateManager
import com.quranapp.android.utils.tafsir.TafsirManager
import com.quranapp.android.utils.translation.TranslationVersionManager
import com.quranapp.android.utils.wbw.WbwManager
import com.quranapp.android.utils.wbw.WbwVersionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

enum class ResourceUpdateState {
    IDLE, CHECKING, UPDATING, COMPLETED, FAILED
}

class ResourceUpdateManager private constructor() {
    companion object {
        private const val DIR_NAME = "other"
        private var INSTANCE: ResourceUpdateManager? = null

        fun getInstance(): ResourceUpdateManager {
            if (INSTANCE == null) {
                INSTANCE = ResourceUpdateManager()
            }
            return INSTANCE!!
        }
    }

    private fun versionsFile() = AppFiles.savedFile(DIR_NAME, "resources_versions.json")

    private val _updateState = MutableStateFlow(ResourceUpdateState.IDLE)
    val updateState: StateFlow<ResourceUpdateState> = _updateState.asStateFlow()


    private fun getLocalVersions(): ResourcesVersions? {
        val file = versionsFile().toFile()
        if (!file.exists() || file.length() == 0L) return null

        return try {
            JsonHelper.json.decodeFromString<ResourcesVersions>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkAndPerformUpdates(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (_updateState.value == ResourceUpdateState.CHECKING || _updateState.value == ResourceUpdateState.UPDATING) return@withContext

        _updateState.value = ResourceUpdateState.CHECKING

        try {
            val remoteVersions = ApiClient.getResourcesVersions()
            val localVersions = getLocalVersions()

            if (force || localVersions == null || isAnyUpdateAvailable(
                    localVersions,
                    remoteVersions
                )
            ) {
                _updateState.value = ResourceUpdateState.UPDATING

                Logger.print("Resources update available: ", remoteVersions)
                performUpdates(localVersions, remoteVersions, force)

                saveLocalVersions(remoteVersions)

                _updateState.value = ResourceUpdateState.COMPLETED
            } else {
                _updateState.value = ResourceUpdateState.IDLE
            }
        } catch (e: Exception) {
            Logger.saveError(e, "ResourceUpdateManager.checkAndPerformUpdates")
            _updateState.value = ResourceUpdateState.FAILED
        }
    }

    private fun isAnyUpdateAvailable(local: ResourcesVersions, remote: ResourcesVersions): Boolean {
        return remote.urlsVersion > local.urlsVersion ||
                remote.translationsVersion > local.translationsVersion ||
                remote.recitationsVersion > local.recitationsVersion ||
                remote.recitationTranslationsVersion > local.recitationTranslationsVersion ||
                remote.tafsirsVersion > local.tafsirsVersion ||
                remote.wbwVersion > local.wbwVersion ||
                remote.wbwAudioVersion > local.wbwAudioVersion ||
                remote.templatesVersion > local.templatesVersion
    }

    private suspend fun performUpdates(
        local: ResourcesVersions?,
        remote: ResourcesVersions,
        force: Boolean
    ) = withContext(Dispatchers.IO) {
        supervisorScope {
            // URLs
            launch {
                if (force || local == null || remote.urlsVersion > local.urlsVersion) {
                    try {
                        UrlsManager().refresh()
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateUrls")
                    }
                }
            }

            // Recitations
            launch {
                if (force || local == null || remote.recitationsVersion > local.recitationsVersion ||
                    remote.recitationTranslationsVersion > local.recitationTranslationsVersion
                ) {
                    try {
                        RecitationModelManager.refreshManifests()
                        RecitationVersionManager.get().reconcileAfterManifestRefresh()
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateRecitations")
                    }
                }
            }

            // Translations
            launch {
                if (force || local == null || remote.translationsVersion > local.translationsVersion) {
                    try {
                        TranslationVersionManager.get().reconcileAfterManifestRefresh(
                            forceFetchManifest = true,
                        )
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateTranslations")
                    }
                }
            }

            // Tafsirs
            launch {
                if (force || local == null || remote.tafsirsVersion > local.tafsirsVersion) {
                    try {
                        TafsirManager.prepare(true) { /* no-op */ }
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateTafsirs")
                    }
                }
            }

            // WBW
            launch {
                if (force || local == null || remote.wbwVersion > local.wbwVersion) {
                    try {
                        WbwManager.getAvailable(forceRefresh = true)
                        WbwVersionManager.get().reconcileAfterManifestRefresh()
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateWbw")
                    }
                }
            }

            // WBW chapter word-audio timings
            launch {
                if (force || local == null || remote.wbwAudioVersion > local.wbwAudioVersion) {
                    try {
                        WbwAudioRepository.refreshTimingsFromRemote()
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateWbwAudio")
                    }
                }
            }

            // templates
            launch {
                if (force || local == null || remote.templatesVersion > local.templatesVersion) {
                    try {
                        VerseTemplateManager.getAvailable(forceRefresh = true)
                    } catch (e: Exception) {
                        Logger.saveError(e, "ResourceUpdateManager.updateTemplates")
                    }
                }
            }
        }
    }

    private fun saveLocalVersions(versions: ResourcesVersions) {
        try {
            val file = versionsFile()
            if (AppFiles.createFile(file)) {
                file.toFile().writeText(JsonHelper.json.encodeToString(versions))
            }
        } catch (e: Exception) {
            Logger.saveError(e, "ResourceUpdateManager.saveLocalVersions")
        }
    }
}
