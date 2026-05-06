package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.ResourcesVersions
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.reader.wbw.WbwManager
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

enum class ResourceUpdateState {
    IDLE, CHECKING, UPDATING, COMPLETED, FAILED
}

class ResourceUpdateManager private constructor(private val ctx: Context) {
    companion object {
        private var INSTANCE: ResourceUpdateManager? = null

        fun getInstance(context: Context): ResourceUpdateManager {
            if (INSTANCE == null) {
                INSTANCE = ResourceUpdateManager(context.applicationContext)
            }
            return INSTANCE!!
        }
    }

    private val _updateState = MutableStateFlow(ResourceUpdateState.IDLE)
    val updateState: StateFlow<ResourceUpdateState> = _updateState.asStateFlow()

    private val fileUtils = FileUtils.newInstance(ctx)

    private fun getLocalVersions(): ResourcesVersions? {
        val file = fileUtils.resourcesVersionsFile
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
            val remoteVersions = RetrofitInstance.github.getResourcesVersions()
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
            Log.saveError(e, "ResourceUpdateManager.checkAndPerformUpdates")
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
                remote.wbwAudioVersion > local.wbwAudioVersion
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
                        UrlsManager(ctx).refresh()
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateUrls")
                    }
                }
            }

            // Recitations
            launch {
                if (force || local == null || remote.recitationsVersion > local.recitationsVersion ||
                    remote.recitationTranslationsVersion > local.recitationTranslationsVersion
                ) {
                    try {
                        RecitationModelManager.get(ctx).refreshManifests()
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateRecitations")
                    }
                }
            }

            // Tafsirs
            launch {
                if (force || local == null || remote.tafsirsVersion > local.tafsirsVersion) {
                    try {
                        TafsirManager.prepare(ctx, true) { /* no-op */ }
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateTafsirs")
                    }
                }
            }

            // WBW
            launch {
                if (force || local == null || remote.wbwVersion > local.wbwVersion) {
                    try {
                        WbwManager.getAvailable(ctx, forceRefresh = true)
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateWbw")
                    }
                }
            }

            // WBW chapter word-audio timings
            launch {
                if (force || local == null || remote.wbwAudioVersion > local.wbwAudioVersion) {
                    try {
                        WbwAudioRepository.refreshTimingsFromRemote(ctx)
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateWbwAudio")
                    }
                }
            }
        }
    }

    private fun saveLocalVersions(versions: ResourcesVersions) {
        try {
            val file = fileUtils.resourcesVersionsFile
            if (fileUtils.createFile(file)) {
                file.writeText(JsonHelper.json.encodeToString(versions))
            }
        } catch (e: Exception) {
            Log.saveError(e, "ResourceUpdateManager.saveLocalVersions")
        }
    }
}
