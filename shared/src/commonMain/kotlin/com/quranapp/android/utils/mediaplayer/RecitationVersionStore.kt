package com.quranapp.android.utils.mediaplayer

import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.PrefKey
import com.quranapp.android.utils.app.Logger
import kotlinx.serialization.Serializable

@Serializable
data class ReciterStoredVersions(
    val audio: Int = 1,
    val timing: Int = 0,
)

object RecitationVersionStore {
    internal val KEY = PrefKey(stringPreferencesKey("reciter_versions"), "")

    suspend fun loadAll(): MutableMap<String, ReciterStoredVersions> {
        val raw = DataStoreManager.readFirst(KEY)
        if (raw.isEmpty()) return mutableMapOf()

        return try {
            JsonHelper.json.decodeFromString<Map<String, ReciterStoredVersions>>(raw).toMutableMap()
        } catch (e: Exception) {
            Logger.saveError(e, "RecitationVersionStore.loadAll")
            mutableMapOf()
        }
    }

    suspend fun saveAll(data: Map<String, ReciterStoredVersions>) {
        DataStoreManager.write(KEY, JsonHelper.json.encodeToString(data))
    }

    suspend fun getStoredVersions(reciterId: String): ReciterStoredVersions? {
        return loadAll()[reciterId]
    }

    suspend fun setStoredVersions(reciterId: String, audio: Int, timing: Int) {
        val all = loadAll()
        all[reciterId] = ReciterStoredVersions(audio = audio, timing = timing)
        saveAll(all)
    }
}
