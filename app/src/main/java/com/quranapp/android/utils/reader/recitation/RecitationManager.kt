package com.quranapp.android.utils.reader.recitation

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.recitation.AvailableRecitationTranslationsModel
import com.quranapp.android.api.models.recitation.AvailableRecitationsModel
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.IOException
import java.util.*

object RecitationManager {
    private var availableRecitationsModel: AvailableRecitationsModel? = null
    private var availableRecitationTranslationsModel: AvailableRecitationTranslationsModel? = null

    @JvmStatic
    fun prepare(
        ctx: Context,
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        if (!force && availableRecitationsModel != null && availableRecitationsModel!!.reciters.isNotEmpty()) {
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
                    recitationsFile.writeText(stringData)

                    withContext(Dispatchers.Main) {
                        postRecitationsLoad(ctx, stringData, callback)
                    }
                } catch (e: Exception) {
                    Log.saveError(e)

                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            }
        } else {
            if (recitationsFile.length() == 0L) {
                loadRecitations(ctx, true, callback)
                return
            }

            try {
                val stringData = recitationsFile.readText()
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
    fun prepareTranslations(
        ctx: Context,
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        if (!force && availableRecitationTranslationsModel != null) {
            readyCallback()
            return
        }

        loadRecitationTranslations(ctx, force) { availableRecitationsModel ->
            availableRecitationTranslationsModel = availableRecitationsModel
            readyCallback()
        }
    }

    private fun loadRecitationTranslations(
        ctx: Context,
        force: Boolean,
        callback: (AvailableRecitationTranslationsModel?) -> Unit
    ) {
        val fileUtils = FileUtils.newInstance(ctx)

        val recitationTranslationsFile = fileUtils.recitationTranslationsManifestFile
        if (force) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stringData = RetrofitInstance.github.getAvailableRecitationTranslations().string()

                    fileUtils.createFile(recitationTranslationsFile)
                    recitationTranslationsFile.writeText(stringData)

                    withContext(Dispatchers.Main) {
                        postRecitationTranslationsLoad(ctx, stringData, callback)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            }
        } else {
            if (!recitationTranslationsFile.exists()) {
                loadRecitationTranslations(ctx, true, callback)
                return
            }

            try {
                val stringData = recitationTranslationsFile.readText()
                if (stringData.isEmpty()) {
                    loadRecitationTranslations(ctx, true, callback)
                    return
                }

                postRecitationTranslationsLoad(ctx, stringData, callback)
            } catch (e: IOException) {
                e.printStackTrace()
                loadRecitationTranslations(ctx, true, callback)
            }
        }
    }

    private fun postRecitationTranslationsLoad(
        ctx: Context,
        stringData: String,
        callback: (AvailableRecitationTranslationsModel?) -> Unit
    ) {
        SPAppActions.setFetchRecitationTranslationsForce(ctx, false)
        val savedSlug = SPReader.getSavedRecitationTranslationSlug(ctx)

        try {
            val model = JsonHelper.json.decodeFromString<AvailableRecitationTranslationsModel>(
                stringData
            )

            model.reciters.forEach { reciterModel ->
                if (reciterModel.urlHost.isNullOrEmpty()) {
                    reciterModel.urlHost = model.urlInfo.commonHost
                }
                reciterModel.langName = Locale(reciterModel.langCode).getDisplayName(Locale.getDefault())
                reciterModel.isChecked = reciterModel.slug == savedSlug
            }

            callback(model)
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    @JvmStatic
    fun getModel(slug: String): RecitationInfoModel? {
        return availableRecitationsModel?.reciters?.firstOrNull { it.slug == slug }
    }

    @JvmStatic
    fun getTranslationModel(slug: String): RecitationTranslationInfoModel? {
        return availableRecitationTranslationsModel?.reciters?.firstOrNull { it.slug == slug }
    }

    @JvmStatic
    fun getModels(): List<RecitationInfoModel>? {
        return availableRecitationsModel?.reciters
    }

    @JvmStatic
    fun getTranslationModels(): List<RecitationTranslationInfoModel>? {
        return availableRecitationTranslationsModel?.reciters
    }

    @JvmStatic
    fun setSavedRecitationSlug(slug: String) {
        availableRecitationsModel?.reciters?.forEach { recitationModel ->
            recitationModel.isChecked = recitationModel.slug == slug
        }
    }

    @JvmStatic
    fun setSavedRecitationTranslationSlug(slug: String) {
        availableRecitationTranslationsModel?.reciters?.forEach { model ->
            model.isChecked = model.slug == slug
        }
    }

    fun emptyModel(
        slug: String = "",
        reciter: String = "",
        style: String? = null,
        urlHost: String? = null,
        urlPath: String = ""
    ): RecitationInfoModel {
        return RecitationInfoModel(
            slug = slug,
            reciter = reciter,
            style = style,
            urlHost = urlHost,
            urlPath = urlPath
        )
    }
}
