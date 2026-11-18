package com.quranapp.android.utils.reader.wbw

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.wbw.AvailableWbwInfoModel
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.app.AppFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path

object WbwManager {
    private const val DIR_NAME = "wbw"
    private const val MANIFEST_FILENAME = "available_wbw_info_v2.json"

    @Volatile
    private var cachedManifest: AvailableWbwInfoModel? = null

    private val lock = Mutex()

    private fun manifestFile(): Path = AppFiles.savedFile(DIR_NAME, MANIFEST_FILENAME)

    fun tempDownloadFile(id: String): Path = AppFiles.savedFile(DIR_NAME, "$id.tmp")

    suspend fun getAvailable(forceRefresh: Boolean = false): AvailableWbwInfoModel? {
        val inMemory = cachedManifest
        if (!forceRefresh && inMemory != null) return inMemory

        return lock.withLock {
            val recheck = cachedManifest
            if (!forceRefresh && recheck != null) return@withLock recheck

            if (!forceRefresh) {
                loadLocal()?.let {
                    cachedManifest = it
                    return@withLock it
                }
            }

            val network = loadNetwork() ?: return@withLock null
            cachedManifest = network
            network
        }
    }

    suspend fun markResourceVersion(id: String, version: Int) {
        WbwVersionStore.setItemVersion(id, version)
    }

    fun getResourceVersion(id: String): Int = WbwVersionStore.getItemVersion(id)

    private suspend fun loadLocal(): AvailableWbwInfoModel? = withContext(Dispatchers.IO) {
        val file = manifestFile()
        if (!AppFiles.exists(file) || AppFiles.size(file) <= 0L) return@withContext null

        return@withContext try {
            JsonHelper.json.decodeFromString<AvailableWbwInfoModel>(AppFiles.readText(file))
        } catch (e: Exception) {
            Logger.saveError(e, "WbwManager.loadLocal")
            null
        }
    }

    private suspend fun loadNetwork(): AvailableWbwInfoModel? = withContext(Dispatchers.IO) {
        val manifest = try {
            ApiClient.getAvailableWbwInfo()
        } catch (e: Exception) {
            Logger.saveError(e, "WbwManager.loadNetwork")
            return@withContext null
        }

        AppFiles.writeText(manifestFile(), JsonHelper.json.encodeToString(manifest))
        manifest
    }
}
