package com.quranapp.android.backup

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quranapp.android.utils.workers.AutoBackupWorker
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

object AutoBackup {
    const val FILE_NAME = "quranapp-backup.json.gz"
    private const val WORK_NAME = "auto_backup"
    private const val PERSISTABLE_URI_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    private val scopes = mapOf(
        ExportKeys.BOOKMARKS to true,
        ExportKeys.COLLECTIONS to true,
        ExportKeys.SETTINGS to true,
    )

    fun schedule(context: Context, restart: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (restart) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    suspend fun ensureAvailable(context: Context) {
        if (!BackupPreferences.isEnabled()) return

        val raw = BackupPreferences.readDocumentUri().takeIf { it.isNotBlank() } ?: return
        val uri = raw.toUri()
        if (displayName(context, uri) == null) forgetMissing(context, uri)
    }

    suspend fun writeNow(context: Context): Boolean {
        ensureAvailable(context)
        if (!BackupPreferences.isEnabled()) return false

        val uri = BackupPreferences.readDocumentUri().toUri()
        val bytes = BackupImportService(context).exportBytes(scopes)

        return try {
            writeToDocument(context.contentResolver, uri, bytes)
            true
        } catch (error: FileNotFoundException) {
            forgetMissing(context, uri)
            false
        } catch (error: SecurityException) {
            forgetMissing(context, uri)
            false
        }
    }

    fun displayName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            ) ?: return null

            cursor.use {
                if (!it.moveToFirst()) return null
                it.getString(0)?.takeIf { name -> name.isNotBlank() } ?: FILE_NAME
            }
        } catch (error: Exception) {
            when (error) {
                is FileNotFoundException, is SecurityException, is IllegalArgumentException -> null
                else -> FILE_NAME
            }
        }
    }

    private suspend fun forgetMissing(context: Context, uri: Uri) {
        if (BackupPreferences.readDocumentUri() != uri.toString()) return

        cancel(context)
        BackupPreferences.clearMissingFile()
        runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, PERSISTABLE_URI_FLAGS)
        }
    }

    fun openFile(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/gzip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private fun writeToDocument(resolver: ContentResolver, uri: Uri, bytes: ByteArray) {
        val stream = try {
            resolver.openOutputStream(uri, "wt")
        } catch (_: IllegalArgumentException) {
            resolver.openOutputStream(uri)
        } ?: error("Could not open backup file")

        stream.use { it.write(bytes) }
    }
}
