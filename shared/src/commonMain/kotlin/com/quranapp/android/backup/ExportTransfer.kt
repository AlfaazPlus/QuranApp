package com.quranapp.android.backup

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
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.platform.files.FileUnavailableException
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.utils.app.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.buffer
import org.jetbrains.compose.resources.getString

private val externalFiles get() = Platform.externalFiles

fun readImportFileName(location: String): String {
    return externalFiles.displayName(location)
        ?: location.substringAfterLast('/').takeIf { it.isNotBlank() }
        ?: "backup.json"
}

suspend fun importBackup(location: String, scopes: Map<String, Boolean>) {
    try {
        val bytes = try {
            externalFiles.source(location).use { BackupFiles.readLimited(it) }
        } catch (_: FileUnavailableException) {
            throw BackupReadException(BackupReadFailure.UNREADABLE)
        }
        val result = BackupImportService().import(bytes, scopes)

        withContext(Dispatchers.Main + NonCancellable) {
            val counts = result.counts
            Platform.messages.show(
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
                ThemeUtils.applyNightMode()

                val localeTag = DataStoreManager.readFirst(AppPreferences.KEY_APP_LANGUAGE)
                val numeral = NumeralSystem.fromStorage(
                    DataStoreManager.readFirst(AppPreferences.KEY_NUMERAL_SYSTEM)
                        .takeIf { it.isNotEmpty() },
                )
                setAppLocale(appLocaleForLanguageChange(localeTag, numeral))

                restartMainActivity()
            }
        }
    } catch (error: Exception) {
        Logger.saveError(error, "importData")
        withContext(Dispatchers.Main) {
            showBackupFailure(error)
        }
    }
}

private fun restartMainActivity() {
    Platform.navigator.navigate(AppRoute.Screen(AppRoutes.MAIN))
}

suspend fun writeExport(location: String, bytes: ByteArray) {
    if (bytes.isEmpty()) return

    try {
        externalFiles.sink(location).buffer().use { it.write(bytes) }
    } catch (_: FileUnavailableException) {
        return
    }

    withContext(Dispatchers.Main) {
        Platform.messages.show(getString(Res.string.msgExportSuccess), long = true)
    }
}

suspend fun showBackupFailure(error: Exception) {
    val message = when ((error as? BackupReadException)?.failure) {
        BackupReadFailure.UNSUPPORTED_VERSION -> Res.string.backupUnsupportedVersion
        BackupReadFailure.TOO_LARGE -> Res.string.backupTooLarge
        else -> Res.string.backupFailed
    }

    Platform.messages.show(getString(message), long = true)
}

suspend fun enableAutoBackup(location: String) {
    if (!externalFiles.retain(location)) {
        withContext(Dispatchers.Main) {
            Platform.messages.show(getString(Res.string.autoBackupFailed), long = true)
        }
        return
    }

    val previous = BackupPreferences.documentUri()
    if (previous.isNotBlank() && previous != location) {
        externalFiles.release(previous)
    }

    BackupPreferences.setDocumentPath(location)
    AutoBackup.schedule(restart = true)

    try {
        val saved = AutoBackup.writeNow()
        withContext(Dispatchers.Main) {
            Platform.messages.show(
                getString(if (saved) Res.string.autoBackupSaved else Res.string.autoBackupFailed),
                long = true,
            )
        }
    } catch (error: Exception) {
        Logger.saveError(error, "autoBackup")
        withContext(Dispatchers.Main) {
            Platform.messages.show(
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
