package com.quranapp.android.utils.tafsir

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.tafsir.AvailableTafsirsModel
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.preferences.AppActionsPreferences
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.translation.isCustomImportSlug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import java.io.IOException

object TafsirManager {
    private var cachedTafsirsModel: AvailableTafsirsModel? = null

    fun prepare(
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        if (!force && cachedTafsirsModel != null) {
            readyCallback()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            TafsirManager.cachedTafsirsModel = loadTafsirs(force)

            withContext(Dispatchers.Main) {
                readyCallback()
            }
        }
    }

    private suspend fun loadTafsirs(
        force: Boolean,
    ): AvailableTafsirsModel? = withContext(Dispatchers.IO) {
        val tafsirsFile = TafsirUtils.manifestFile().toFile()

        if (force) {
            try {
                val stringData = ApiClient.getAvailableTafsirs()

                AppFiles.createFile(tafsirsFile.toOkioPath())
                tafsirsFile.writeText(stringData)

                return@withContext postTafsirsLoad(stringData)
            } catch (e: Exception) {
                Logger.saveError(e, "loadTafsirs")
                return@withContext null
            }
        } else {
            if (!tafsirsFile.exists()) {
                return@withContext loadTafsirs(true)
            }

            try {
                val stringData = tafsirsFile.readText()
                if (stringData.isEmpty()) {
                    return@withContext loadTafsirs(true)
                }

                return@withContext postTafsirsLoad(stringData)
            } catch (e: IOException) {
                Logger.saveError(e, "loadTafsirs")
                return@withContext loadTafsirs(true)
            }
        }
    }

    private suspend fun postTafsirsLoad(
        stringData: String,
    ): AvailableTafsirsModel? {
        AppActionsPreferences.setFetchTafsirsForce(false)
        val savedTafsirKey = ReaderPreferences.getTafsirId()

        try {
            val loaded = JsonHelper.json.decodeFromString<AvailableTafsirsModel>(stringData)
            loaded.tafsirs.values.forEach { tafsirModels ->
                tafsirModels.forEach { tafsirModel ->
                    tafsirModel.isChecked = tafsirModel.key == savedTafsirKey
                }
            }

            return withCustom(loaded)
        } catch (e: Exception) {
            Logger.saveError(e, "postTafsirsLoad")
            e.printStackTrace()
            return null
        }
    }

    fun add(info: TafsirInfoModel) {
        val current = cachedTafsirsModel ?: return
        cachedTafsirsModel = withInfos(current, listOf(info))
    }

    fun remove(key: String) {
        val current = cachedTafsirsModel ?: return
        cachedTafsirsModel = current.copy(
            tafsirs = current.tafsirs
                .mapValues { (_, tafsirs) -> tafsirs.filter { it.key != key } }
                .filterValues { it.isNotEmpty() }
        )
    }

    private fun withCustom(model: AvailableTafsirsModel): AvailableTafsirsModel {
        val dbHelper = QuranTafsirDBHelper()

        try {
            val custom = dbHelper.getStoredTafsirInfos().filter { isCustomImportSlug(it.key) }
            return withInfos(model, custom)
        } finally {
            dbHelper.close()
        }
    }

    private fun withInfos(
        model: AvailableTafsirsModel,
        infos: List<TafsirInfoModel>,
    ): AvailableTafsirsModel {
        if (infos.isEmpty()) return model
        val grouped =
            model.tafsirs.mapValues { (_, tafsirs) -> tafsirs.toMutableList() }.toMutableMap()

        for (info in infos) {
            val list = grouped.getOrPut(info.langCode) { mutableListOf() }
            list.removeAll { it.key == info.key }
            list.add(info)
        }

        return model.copy(tafsirs = grouped)
    }

    fun getModel(key: String): TafsirInfoModel? {
        val tafsirListForLangCodes = cachedTafsirsModel?.tafsirs?.values ?: return null

        for (tafsirList in tafsirListForLangCodes) {
            val tafsir = tafsirList.firstOrNull { it.key == key }
            if (tafsir != null) return tafsir
        }

        return null
    }

    fun getModels(): Map<String, List<TafsirInfoModel>>? {
        return cachedTafsirsModel?.tafsirs
    }

    fun getModels(lang: String?): List<TafsirInfoModel>? {
        return cachedTafsirsModel?.tafsirs?.get(lang!!)
    }

    fun setSavedTafsirKey(key: String) {
        cachedTafsirsModel?.tafsirs?.values?.forEach { tafsirModels ->
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
        )
    }
}
