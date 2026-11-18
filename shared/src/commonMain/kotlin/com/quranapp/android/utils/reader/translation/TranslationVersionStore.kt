package com.quranapp.android.utils.reader.translation

import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.PrefKey
import com.quranapp.android.utils.app.Logger
import kotlinx.serialization.Serializable

@Serializable
data class TranslationVersionEntry(
    val version: Long = 0,
)

object TranslationVersionStore {
    internal val KEY = PrefKey(stringPreferencesKey("translation_versions"), "")

    suspend fun loadAll(): MutableMap<String, TranslationVersionEntry> {
        val raw = DataStoreManager.readFirst(KEY)
        if (raw.isEmpty()) return mutableMapOf()

        return try {
            JsonHelper.json.decodeFromString<Map<String, TranslationVersionEntry>>(raw).toMutableMap()
        } catch (e: Exception) {
            Logger.saveError(e, "TranslationVersionStore.loadAll")
            mutableMapOf()
        }
    }

    suspend fun saveAll(data: Map<String, TranslationVersionEntry>) {
        DataStoreManager.write(KEY, JsonHelper.json.encodeToString(data))
    }
}
