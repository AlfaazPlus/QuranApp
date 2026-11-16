package com.quranapp.android.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import com.quranapp.android.core.R
import com.quranapp.android.locale.NumeralSystem
import com.quranapp.android.locale.appLocaleForLanguageChange
import com.quranapp.android.locale.setAppLocale
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val PERSISTABLE_URI_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

fun readImportFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }

    return uri.lastPathSegment ?: "backup.json"
}

suspend fun importBackup(context: Context, uri: Uri, scopes: Map<String, Boolean>) {
    try {
        val bytes =
            context.contentResolver.openInputStream(uri)?.use { BackupFiles.readLimited(it) }
                ?: throw BackupReadException(BackupReadFailure.UNREADABLE)
        val result = BackupImportService(context).import(bytes, scopes)

        withContext(Dispatchers.Main + NonCancellable) {
            val counts = result.counts
            PlatformServices.messages.show(
                context.getString(
                    R.string.importSummary,
                    counts.inserted,
                    counts.updated,
                    counts.unchanged,
                    counts.skipped,
                    counts.conflicts,
                    result.unsupportedSettings.size,
                ),
                long = true,
            )

            if (result.settingsImported) {
                AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeForDelegate())

                val numeral = NumeralSystem.fromStorage(SPAppConfigs.getNumeralSystem(context))
                setAppLocale(
                    context,
                    appLocaleForLanguageChange(context, SPAppConfigs.getLocale(context), numeral),
                )

                restartMainActivity(context)
            }
        }
    } catch (error: Exception) {
        Log.saveError(error, "importData")
        withContext(Dispatchers.Main) {
            showBackupFailure(context, error)
        }
    }
}

private fun restartMainActivity(context: Context) {
    val intent = AppDestinations.intent(context, AppDestinations.MAIN)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
}

suspend fun writeExport(context: Context, uri: Uri, bytes: ByteArray) {
    if (bytes.isEmpty()) return

    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }

    withContext(Dispatchers.Main) {
        PlatformServices.messages.show(R.string.msgExportSuccess, long = true)
    }
}

fun showBackupFailure(context: Context, error: Exception) {
    val message = when ((error as? BackupReadException)?.failure) {
        BackupReadFailure.UNSUPPORTED_VERSION -> R.string.backupUnsupportedVersion
        BackupReadFailure.TOO_LARGE -> R.string.backupTooLarge
        else -> R.string.backupFailed
    }

    PlatformServices.messages.show(message, long = true)
}

fun persistBackupFile(context: Context, uri: Uri, grantedFlags: Int): Boolean {
    val flags = grantedFlags and PERSISTABLE_URI_FLAGS
    if (flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0) return false

    return try {
        val previous = BackupPreferences.documentUri()
        context.contentResolver.takePersistableUriPermission(uri, flags)

        if (previous.isNotBlank() && previous != uri.toString()) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    previous.toUri(),
                    PERSISTABLE_URI_FLAGS,
                )
            }
        }

        true
    } catch (error: SecurityException) {
        Log.saveError(error, "autoBackup")
        false
    }
}

suspend fun enableAutoBackup(context: Context, documentUri: String) {
    BackupPreferences.setDocumentUri(documentUri)
    AutoBackup.schedule(context, restart = true)

    try {
        val saved = AutoBackup.writeNow(context)
        withContext(Dispatchers.Main) {
            PlatformServices.messages.show(
                if (saved) R.string.autoBackupSaved else R.string.autoBackupFailed,
                long = true,
            )
        }
    } catch (error: Exception) {
        Log.saveError(error, "autoBackup")
        withContext(Dispatchers.Main) {
            PlatformServices.messages.show(R.string.autoBackupFailed, long = true)
        }
    }
}

suspend fun disableAutoBackup(context: Context) {
    AutoBackup.cancel(context)
    BackupPreferences.setEnabled(false)
}
