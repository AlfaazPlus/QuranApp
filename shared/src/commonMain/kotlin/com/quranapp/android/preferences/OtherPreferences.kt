package com.quranapp.android.preferences

import androidx.datastore.preferences.core.intPreferencesKey

object OtherPreferences {
    private val KEY_TRANSLATION_FTS_SCHEMA = PrefKey(intPreferencesKey("translation_fts_schema"), 0)

    fun getTranslationFtsSchemaVersion(): Int {
        return DataStoreManager.read(KEY_TRANSLATION_FTS_SCHEMA)
    }

    suspend fun setTranslationFtsSchemaVersion(version: Int) {
        return DataStoreManager.write(KEY_TRANSLATION_FTS_SCHEMA, version)
    }
}
