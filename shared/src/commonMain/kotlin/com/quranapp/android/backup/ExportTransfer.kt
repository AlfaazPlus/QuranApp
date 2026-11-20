package com.quranapp.android.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.autoBackupFailed
import com.quranapp.android.generated.resources.autoBackupSaved
import com.quranapp.android.generated.resources.backupFailed
import com.quranapp.android.generated.resources.backupTooLarge
import com.quranapp.android.generated.resources.backupUnsupportedVersion
import com.quranapp.android.generated.resources.importSummary
import com.quranapp.android.generated.resources.msgExportSuccess
import com.quranapp.android.locale.NumeralSystem
import com.quranapp.android.locale.appLocaleForLanguageChange
import com.quranapp.android.locale.setAppLocale
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.resolveThemeModeForDelegate
import com.quranapp.android.utils.app.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.source
import org.jetbrains.compose.resources.getString
import com.quranapp.android.navigation.AppRoutes

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
            context.contentResolver.openInputStream(uri)
                ?.use { BackupFiles.readLimited(it.source()) }
                ?: throw BackupReadException(BackupReadFailure.UNREADABLE)
        val result = BackupImportService().import(bytes, scopes)

        withContext(Dispatchers.Main + NonCancellable) {
            val counts = result.counts
            PlatformServices.messages.show(
                getString(
                    Res.string.importSummary,
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

                val localeTag = DataStoreManager.readFirst(AppPreferences.KEY_APP_LANGUAGE)
                val numeral = NumeralSystem.fromStorage(
                    DataStoreManager.readFirst(AppPreferences.KEY_NUMERAL_SYSTEM)
                        .takeIf { it.isNotEmpty() },
                )
                setAppLocale(
                    context,
                    appLocaleForLanguageChange(context, localeTag, numeral),
                )

                restartMainActivity(context)
            }
        }
    } catch (error: Exception) {
        Logger.saveError(error, "importData")
        withContext(Dispatchers.Main) {
            showBackupFailure(context, error)
        }
    }
}

private fun restartMainActivity(context: Context) {
    PlatformServices.navigator.navigate(AppRoute.Screen(AppRoutes.MAIN))
}

suspend fun writeExport(context: Context, uri: Uri, bytes: ByteArray) {
    if (bytes.isEmpty()) return

    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }

    withContext(Dispatchers.Main) {
        PlatformServices.messages.show(getString(Res.string.msgExportSuccess), long = true)
    }
}

suspend fun showBackupFailure(context: Context, error: Exception) {
    val message = when ((error as? BackupReadException)?.failure) {
        BackupReadFailure.UNSUPPORTED_VERSION -> Res.string.backupUnsupportedVersion
        BackupReadFailure.TOO_LARGE -> Res.string.backupTooLarge
        else -> Res.string.backupFailed
    }

    PlatformServices.messages.show(getString(message), long = true)
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
        Logger.saveError(error, "autoBackup")
        false
    }
}

suspend fun enableAutoBackup(documentPath: String) {
    BackupPreferences.setDocumentPath(documentPath)
    AutoBackup.schedule(restart = true)

    try {
        val saved = AutoBackup.writeNow()
        withContext(Dispatchers.Main) {
            PlatformServices.messages.show(
                getString(if (saved) Res.string.autoBackupSaved else Res.string.autoBackupFailed),
                long = true,
            )
        }
    } catch (error: Exception) {
        Logger.saveError(error, "autoBackup")
        withContext(Dispatchers.Main) {
            PlatformServices.messages.show(
                getString(Res.string.autoBackupFailed),
                long = true
            )
        }
    }
}

suspend fun disableAutoBackup() {
    AutoBackup.cancel()
    BackupPreferences.setEnabled(false)
}
