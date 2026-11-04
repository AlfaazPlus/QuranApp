package com.quranapp.android.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

private const val PERSISTABLE_URI_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

internal fun readImportFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }

    return uri.lastPathSegment ?: "backup.json"
}

internal suspend fun importBackup(context: Context, uri: Uri, scopes: Map<String, Boolean>) {
    try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { BackupFiles.readLimited(it) }
            ?: throw BackupReadException(BackupReadFailure.UNREADABLE)
        val result = BackupImportService(context).import(bytes, scopes)

        withContext(Dispatchers.Main) {
            val counts = result.counts
            MessageUtils.showRemovableToast(
                context,
                context.getString(
                    R.string.importSummary,
                    counts.inserted,
                    counts.updated,
                    counts.unchanged,
                    counts.skipped,
                    counts.conflicts,
                    result.unsupportedSettings.size,
                ),
                Toast.LENGTH_LONG,
            )

            if (result.settingsImported) context.findBaseActivity()?.restartMainActivity()
        }
    } catch (error: Exception) {
        Log.saveError(error, "importData")
        withContext(Dispatchers.Main) {
            showBackupFailure(context, error)
        }
    }
}

internal suspend fun writeExport(context: Context, uri: Uri, bytes: ByteArray) {
    if (bytes.isEmpty()) return

    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }

    withContext(Dispatchers.Main) {
        MessageUtils.showRemovableToast(context, R.string.msgExportSuccess, Toast.LENGTH_LONG)
    }
}

internal fun showBackupFailure(context: Context, error: Exception) {
    val message = when ((error as? BackupReadException)?.failure) {
        BackupReadFailure.UNSUPPORTED_VERSION -> R.string.backupUnsupportedVersion
        BackupReadFailure.TOO_LARGE -> R.string.backupTooLarge
        else -> R.string.backupFailed
    }

    MessageUtils.showRemovableToast(context, message, Toast.LENGTH_LONG)
}

internal fun persistBackupFile(context: Context, uri: Uri, grantedFlags: Int): Boolean {
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

internal suspend fun enableAutoBackup(context: Context, documentUri: String) {
    BackupPreferences.setDocumentUri(documentUri)
    AutoBackup.schedule(context, restart = true)

    try {
        val saved = AutoBackup.writeNow(context)
        withContext(Dispatchers.Main) {
            MessageUtils.showRemovableToast(
                context,
                if (saved) R.string.autoBackupSaved else R.string.autoBackupFailed,
                Toast.LENGTH_LONG,
            )
        }
    } catch (error: Exception) {
        Log.saveError(error, "autoBackup")
        withContext(Dispatchers.Main) {
            MessageUtils.showRemovableToast(context, R.string.autoBackupFailed, Toast.LENGTH_LONG)
        }
    }
}

internal suspend fun disableAutoBackup(context: Context) {
    AutoBackup.cancel(context)
    BackupPreferences.setEnabled(false)
}

private tailrec fun Context.findBaseActivity(): BaseActivity? {
    return when (this) {
        is BaseActivity -> this
        is ContextWrapper -> baseContext.findBaseActivity()
        else -> null
    }
}
