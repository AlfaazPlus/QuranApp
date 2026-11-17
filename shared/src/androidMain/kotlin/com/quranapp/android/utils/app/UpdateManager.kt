/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/3/2022.
 * All rights reserved.
 */
package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.AppUpdate
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.shared.BuildConfig
import com.quranapp.android.utils.files.AppFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpdateBannerDecision(
    val priority: Int,
    val showCriticalDialog: Boolean,
    val showMajorDialog: Boolean,
    val showInlineBanner: Boolean,
)

class UpdateManager private constructor(private val ctx: Context) {
    companion object {
        const val CRITICAL = 5
        const val MAJOR = 4
        const val MODERATE = 3
        const val MINOR = 2
        const val COSMETIC = 1
        const val NONE = 0

        private const val DIR_NAME = "other"

        private var INSTANCE: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            if (INSTANCE == null) {
                INSTANCE = UpdateManager(context)
            }

            return INSTANCE!!
        }
    }

    private fun updatesFile() = AppFiles.savedFile(DIR_NAME, "app_updates.json")

    private val _bannerDecision = MutableStateFlow(getBannerDecision())
    val bannerDecision: StateFlow<UpdateBannerDecision> = _bannerDecision.asStateFlow()

    init {
        refreshAppUpdatesJson()
    }

    fun refreshAppUpdatesJson() {
        CoroutineScope(Dispatchers.IO).launch { fetchAndSaveUpdates() }
    }

    private suspend fun fetchAndSaveUpdates() {
        withContext(Dispatchers.IO) {
            try {
                val updates = ApiClient.getAppUpdates()
                val updatesString = JsonHelper.json.encodeToString(updates)
                Logger.print("updatesString: $updatesString")

                val updatesFile = updatesFile()
                if (AppFiles.createFile(updatesFile)) {
                    updatesFile.toFile().writeText(updatesString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _bannerDecision.value = getBannerDecision()

        ResourceUpdateManager.getInstance(ctx).checkAndPerformUpdates()
    }

    fun check4CriticalUpdate(): Boolean {
        val decision = getBannerDecision()

        if (decision.showCriticalDialog) {
            Logger.print("UpdateManager:", "Critical update available")
            return true
        }

        return false
    }

    fun getBannerDecision(): UpdateBannerDecision {
        val priority = getMostImportantUpdate().priority

        return UpdateBannerDecision(
            priority = priority,
            showCriticalDialog = priority == CRITICAL,
            showMajorDialog = priority == MAJOR,
            showInlineBanner = priority in MAJOR downTo COSMETIC,
        )
    }

    private fun getMostImportantUpdate(): AppUpdate {
        val currentAppVersion = BuildConfig.VERSION_CODE.toLong()

        return getAvailableUpdates()
            .filter { it.version > currentAppVersion }
            .sortedByDescending { it.priority }
            .firstOrNull() ?: AppUpdate(0, NONE)
    }

    private fun getAvailableUpdates(): List<AppUpdate> {
        return try {
            val file = updatesFile().toFile()

            if (file.exists()) {
                JsonHelper.json.decodeFromString(file.readText())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun openPlayStore() {
        PlatformServices.links.openStore()
    }
}
