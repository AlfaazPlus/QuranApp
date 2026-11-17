package com.quranapp.android.utils.reader.translation

import com.quranapp.android.api.JsonHelper
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.files.AppFiles
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath
import java.io.File

@Serializable
data class TranslationVersionEntry(
    val version: Long = 0,
)

class TranslationVersionStore {
    private fun getStoreFile(): File {
        return AppFiles.savedFile("other", "translation_versions.json").toFile()
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
            Logger.saveError(e, "TranslationVersionStore.loadAll")
            mutableMapOf()
        }
    }

    fun saveAll(data: Map<String, TranslationVersionEntry>) {
        val file = getStoreFile()
        try {
            if (!AppFiles.createFile(file.toOkioPath())) return
            file.writeText(JsonHelper.json.encodeToString(data), Charsets.UTF_8)
        } catch (e: Exception) {
            Logger.saveError(e, "TranslationVersionStore.saveAll")
        }
    }
}
