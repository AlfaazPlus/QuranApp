package com.quranapp.android.utils.reader.tafsir

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.tafsir.AvailableTafsirsModel
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
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

object TafsirManager {
    private var availableTafsirsModel: AvailableTafsirsModel? = null

    @JvmStatic
    fun prepare(
        ctx: Context,
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        if (!force && availableTafsirsModel != null) {
            readyCallback()
            return
        }

        loadTafsirs(ctx, force) { availableTafsirsModel ->
            TafsirManager.availableTafsirsModel = availableTafsirsModel
            readyCallback()
        }
    }


    private fun loadTafsirs(
        ctx: Context,
        force: Boolean,
        callback: (AvailableTafsirsModel?) -> Unit
    ) {
        val fileUtils = FileUtils.newInstance(ctx)

        val tafsirsFile = fileUtils.tafsirsManifestFile
        if (force) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stringData = RetrofitInstance.github.getAvailableTafsirs().string()

                    fileUtils.createFile(tafsirsFile)
                    tafsirsFile.writeText(stringData)

                    withContext(Dispatchers.Main) {
                        postTafsirsLoad(ctx, stringData, callback)
                    }
                } catch (e: Exception) {
                    Log.saveError(e, "loadTafsirs")
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            }
        } else {
            if (!tafsirsFile.exists()) {
                loadTafsirs(ctx, true, callback)
                return
            }

            try {
                val stringData = tafsirsFile.readText()
                if (stringData.isEmpty()) {
                    loadTafsirs(ctx, true, callback)
                    return
                }

                postTafsirsLoad(ctx, stringData, callback)
            } catch (e: IOException) {
                Log.saveError(e, "loadTafsirs")
                e.printStackTrace()
                loadTafsirs(ctx, true, callback)
            }
        }
    }

    private fun postTafsirsLoad(
        ctx: Context,
        stringData: String,
        callback: (AvailableTafsirsModel?) -> Unit
    ) {
        SPAppActions.setFetchTafsirsForce(ctx, false)
        val savedTafsirKey = SPReader.getSavedTafsirKey(ctx)

        try {
            val availableTafsirsModel = JsonHelper.json.decodeFromString<AvailableTafsirsModel>(
                stringData
            )

            availableTafsirsModel.tafsirs.values.forEach { tafsirModels ->
                tafsirModels.forEach { tafsirModel ->
                    tafsirModel.isChecked = tafsirModel.key == savedTafsirKey
                }

            }

            callback(availableTafsirsModel)
        } catch (e: Exception) {
            Log.saveError(e, "postTafsirsLoad")
            e.printStackTrace()
            callback(null)
        }
    }

    @JvmStatic
    fun getModel(key: String): TafsirInfoModel? {
        val tafsirListForLangCodes = availableTafsirsModel?.tafsirs?.values ?: return null

        for (tafsirList in tafsirListForLangCodes) {
            val tafsir = tafsirList.firstOrNull { it.key == key }
            if (tafsir != null) return tafsir
        }

        return null
    }

    @JvmStatic
    fun getModels(): Map<String, List<TafsirInfoModel>>? {
        return availableTafsirsModel?.tafsirs
    }


    @JvmStatic
    fun getModels(lang: String?): List<TafsirInfoModel>? {
        return availableTafsirsModel?.tafsirs?.get(lang!!)
    }


    @JvmStatic
    fun setSavedTafsirKey(key: String) {
        availableTafsirsModel?.tafsirs?.values?.forEach { tafsirModels ->
            tafsirModels.forEach { tafsirModel ->
                tafsirModel.isChecked = tafsirModel.key == key
            }
        }
    }


    fun emptyModel(
        key: String = "",
        name: String = "",
        author: String = "",
        langCode: String = "",
        langName: String = "",
        slug: String = "",
    ): TafsirInfoModel {
        return TafsirInfoModel(
            key = key,
            name = name,
            author = author,
            langCode = langCode,
            langName = langName,
            slug = slug,
        )
    }
}