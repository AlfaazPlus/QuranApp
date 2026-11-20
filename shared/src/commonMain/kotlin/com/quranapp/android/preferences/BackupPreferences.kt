package com.quranapp.android.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object BackupPreferences {
    val enabledKey = PrefKey(booleanPreferencesKey("auto_backup_enabled"), false)
    val documentPathKey = PrefKey(stringPreferencesKey("auto_backup_document_uri"), "")
    val fileDeletedAlertKey =
        PrefKey(booleanPreferencesKey("auto_backup_file_deleted_alert"), false)

    fun documentUri(): String = DataStoreManager.read(documentPathKey)

    suspend fun setEnabled(enabled: Boolean) {
        DataStoreManager.write(enabledKey, enabled)
    }

    suspend fun isEnabled(): Boolean = DataStoreManager.readFirst(enabledKey)

    suspend fun readDocumentPath(): String = DataStoreManager.readFirst(documentPathKey)

    suspend fun setDocumentPath(path: String) {
        DataStoreManager.edit {
            this[enabledKey.key] = true
            this[documentPathKey.key] = path
            this[fileDeletedAlertKey.key] = false
        }
    }

    suspend fun clearMissingFile() {
        DataStoreManager.edit {
            this[enabledKey.key] = false
            this[documentPathKey.key] = ""
            this[fileDeletedAlertKey.key] = true
        }
    }

    suspend fun dismissFileDeletedAlert() {
        DataStoreManager.write(fileDeletedAlertKey, false)
    }

    @Composable
    fun observeEnabled(): Boolean = DataStoreManager.observe(enabledKey)

    @Composable
    fun observeDocumentPath(): String = DataStoreManager.observe(documentPathKey)

    @Composable
    fun observeFileDeletedAlert(): Boolean = DataStoreManager.observe(fileDeletedAlertKey)
}
