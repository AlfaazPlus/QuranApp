package com.quranapp.android.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.PrefKey

object BackupPreferences {
    val enabledKey = PrefKey(booleanPreferencesKey("auto_backup_enabled"), false)
    val documentUriKey = PrefKey(stringPreferencesKey("auto_backup_document_uri"), "")
    val fileDeletedAlertKey =
        PrefKey(booleanPreferencesKey("auto_backup_file_deleted_alert"), false)

    fun documentUri(): String = DataStoreManager.read(documentUriKey)

    suspend fun setEnabled(enabled: Boolean) {
        DataStoreManager.write(enabledKey, enabled)
    }

    suspend fun isEnabled(): Boolean = DataStoreManager.readFirst(enabledKey)

    suspend fun readDocumentUri(): String = DataStoreManager.readFirst(documentUriKey)


    suspend fun setDocumentUri(uri: String) {
        DataStoreManager.edit {
            this[enabledKey.key] = true
            this[documentUriKey.key] = uri
            this[fileDeletedAlertKey.key] = false
        }
    }

    suspend fun clearMissingFile() {
        DataStoreManager.edit {
            this[enabledKey.key] = false
            this[documentUriKey.key] = ""
            this[fileDeletedAlertKey.key] = true
        }
    }

    suspend fun dismissFileDeletedAlert() {
        DataStoreManager.write(fileDeletedAlertKey, false)
    }
}
