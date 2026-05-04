package com.quranapp.android.utils.reader.wbw

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.wbw.AvailableWbwInfoModel
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File

object WbwManager {
    private const val DIR_NAME = "wbw"
    private const val MANIFEST_FILENAME = "available_wbw_info_v2.json"

    private val ROOT_DIR_PATH: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        DIR_NAME
    )

    @Volatile
    private var cachedManifest: AvailableWbwInfoModel? = null

    private val lock = Mutex()

    private fun getRootDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, ROOT_DIR_PATH)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getManifestFile(context: Context): File {
        return File(getRootDir(context), MANIFEST_FILENAME)
    }

    fun getTempDownloadFile(context: Context, id: String): File {
        return File(getRootDir(context), "${id}.tmp")
    }

    suspend fun getAvailable(
        context: Context,
        forceRefresh: Boolean = false
    ): AvailableWbwInfoModel? {
        val inMemory = cachedManifest
        if (!forceRefresh && inMemory != null) {
            return inMemory
        }

        return lock.withLock {
            val recheck = cachedManifest
            if (!forceRefresh && recheck != null) {
                return@withLock recheck
            }

            if (!forceRefresh) {
                loadLocal(context)?.let {
                    cachedManifest = it
                    return@withLock it
                }
            }

            val network = loadNetwork(context) ?: return@withLock null
            cachedManifest = network
            network
        }
    }

    fun markResourceVersion(
        context: Context,
        id: String,
        version: Int
    ) {
        WbwVersionStore(context).setItemVersion(id, version)
    }

    fun getResourceVersion(
        context: Context,
        id: String
    ): Int {
        return WbwVersionStore(context).getItemVersion(id)
    }

    private suspend fun loadLocal(
        context: Context
    ): AvailableWbwInfoModel? = withContext(Dispatchers.IO) {
        val file = getManifestFile(context)
        if (!file.exists() || file.length() <= 0) return@withContext null

        return@withContext try {
            JsonHelper.json.decodeFromString<AvailableWbwInfoModel>(file.readText())
        } catch (e: Exception) {
            Log.saveError(e, "WbwManager.loadLocal")
            null
        }
    }

    private suspend fun loadNetwork(
        context: Context
    ): AvailableWbwInfoModel? = withContext(Dispatchers.IO) {
        val manifest = try {
            RetrofitInstance.github.getAvailableWbwInfo()
        } catch (e: Exception) {
            Log.saveError(e, "WbwManager.loadNetwork")
            return@withContext null
        }

        val file = getManifestFile(context)
        file.parentFile?.mkdirs()
        file.writeText(JsonHelper.json.encodeToString(manifest))

        manifest
    }
}
