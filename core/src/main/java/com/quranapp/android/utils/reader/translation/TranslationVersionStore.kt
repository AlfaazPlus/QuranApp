package com.quranapp.android.utils.reader.translation

import com.quranapp.android.api.JsonHelper
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.files.AppFiles
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class TranslationVersionEntry(
    val version: Long = 0,
)

class TranslationVersionStore {
    private fun getStoreFile(): File {
        val dir = AppFiles.resourceDir(AppUtils.APP_OTHER_DIR)
        return File(dir, "translation_versions.json")
    }

    fun loadAll(): MutableMap<String, TranslationVersionEntry> {
        val file = getStoreFile()
        if (!file.exists() || file.length() == 0L) {
            return mutableMapOf()
        }

        return try {
            JsonHelper.json.decodeFromString<Map<String, TranslationVersionEntry>>(
                file.readText(Charsets.UTF_8),
            ).toMutableMap()
        } catch (e: Exception) {
            Log.saveError(e, "TranslationVersionStore.loadAll")
            mutableMapOf()
        }
    }

    fun saveAll(data: Map<String, TranslationVersionEntry>) {
        val file = getStoreFile()
        try {
            if (!AppFiles.createFile(file)) return
            file.writeText(JsonHelper.json.encodeToString(data), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.saveError(e, "TranslationVersionStore.saveAll")
        }
    }
}
