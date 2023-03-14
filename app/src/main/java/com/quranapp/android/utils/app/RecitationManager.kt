package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.AvailableRecitationsModel
import com.quranapp.android.components.recitation.RecitationModel
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.IOException

object RecitationManager {
    private var availableRecitationsModel: AvailableRecitationsModel? = null

    @JvmStatic
    fun prepare(
        ctx: Context,
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        if (!force && availableRecitationsModel != null) {
            readyCallback()
            return
        }

        loadRecitations(ctx, force) { availableRecitationsModel ->
            RecitationManager.availableRecitationsModel = availableRecitationsModel
            readyCallback()
        }
    }

    private fun loadRecitations(
        ctx: Context,
        force: Boolean,
        callback: (AvailableRecitationsModel?) -> Unit
    ) {
        val fileUtils = FileUtils.newInstance(ctx)

        val recitationsFile = fileUtils.recitationsManifestFile
        if (force) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stringData = RetrofitInstance.github.getAvailableRecitations().string()

                    fileUtils.createFile(recitationsFile)
                    fileUtils.writeToFile(recitationsFile, stringData)

                    withContext(Dispatchers.Main) {
                        postRecitationsLoad(ctx, stringData, callback)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            }
        } else {
            if (!recitationsFile.exists()) {
                loadRecitations(ctx, true, callback)
                return
            }

            try {
                val stringData = fileUtils.readFile(recitationsFile)
                if (stringData.isEmpty()) {
                    loadRecitations(ctx, true, callback)
                    return
                }

                postRecitationsLoad(ctx, stringData, callback)
            } catch (e: IOException) {
                e.printStackTrace()
                loadRecitations(ctx, true, callback)
            }
        }
    }

    private fun postRecitationsLoad(
        ctx: Context,
        stringData: String,
        callback: (AvailableRecitationsModel?) -> Unit
    ) {
        SPAppActions.setFetchRecitationsForce(ctx, false)
        val savedRecitationSlug = SPReader.getSavedRecitationSlug(ctx)

        try {
            val availableRecitationsModel = JsonHelper.json.decodeFromString<AvailableRecitationsModel>(
                stringData
            )

            availableRecitationsModel.reciters.forEach { recitationModel ->
                if (recitationModel.urlHost.isNullOrEmpty()) {
                    recitationModel.urlHost = availableRecitationsModel.urlInfo.commonHost
                }

                recitationModel.isChecked = recitationModel.slug == savedRecitationSlug
            }

            callback(availableRecitationsModel)
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    @JvmStatic
    fun getModel(slug: String): RecitationModel? {
        return availableRecitationsModel?.reciters?.firstOrNull { it.slug == slug }
    }

    @JvmStatic
    fun getModels(): List<RecitationModel>? {
        return availableRecitationsModel?.reciters
    }

    @JvmStatic
    fun setSavedRecitationSlug(slug: String) {
        availableRecitationsModel?.reciters?.forEach { recitationModel ->
            recitationModel.isChecked = recitationModel.slug == slug
        }
    }

    fun emptyModel(
        slug: String = "",
        reciter: String = "",
        style: String? = null,
        urlHost: String? = null,
        urlPath: String = "",
    ): RecitationModel {
        return RecitationModel(
            slug = slug,
            reciter = reciter,
            style = style,
            urlHost = urlHost,
            urlPath = urlPath
        )
    }
}
