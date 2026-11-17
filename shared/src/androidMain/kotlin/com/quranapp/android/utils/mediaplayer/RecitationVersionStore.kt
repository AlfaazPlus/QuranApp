package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.JsonHelper
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.files.AppFiles
import com.quranapp.android.utils.mediaplayer.RecitationModelManager.getRecitationsDir
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath
import java.io.File

@Serializable
data class ReciterStoredVersions(
    val audio: Int = 1,
    val timing: Int = 0,
)

class RecitationVersionStore {
    private fun getStoreFile(): File {
        return (getRecitationsDir() / "reciter_versions.json").toFile()
    }

    fun loadAll(): MutableMap<String, ReciterStoredVersions> {
        val file = getStoreFile()

        if (!file.exists() || file.length() == 0L) {
            return mutableMapOf()
        }

        return try {
            JsonHelper.json.decodeFromString<Map<String, ReciterStoredVersions>>(
                file.readText(Charsets.UTF_8),
            ).toMutableMap()
        } catch (e: Exception) {
            Logger.saveError(e, "RecitationVersionStore.loadAll")
            mutableMapOf()
        }
    }

    fun saveAll(data: Map<String, ReciterStoredVersions>) {
        val file = getStoreFile()
        try {
            if (!AppFiles.createFile(file.toOkioPath())) return
            file.writeText(
                JsonHelper.json.encodeToString(data),
                Charsets.UTF_8,
            )
        } catch (e: Exception) {
            Logger.saveError(e, "RecitationVersionStore.saveAll")
        }
    }

    fun getStoredVersions(reciterId: String): ReciterStoredVersions? {
        return loadAll()[reciterId]
    }

    fun setStoredVersions(reciterId: String, audio: Int, timing: Int) {
        val all = loadAll()
        all[reciterId] = ReciterStoredVersions(audio = audio, timing = timing)
        saveAll(all)
    }
}
