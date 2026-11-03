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
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

object AutoBackup {
    const val FILE_NAME = "quranapp-backup.json.gz"
    private const val WORK_NAME = "auto_backup"

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

    suspend fun writeNow(context: Context) {
        val documentUri = BackupPreferences.readDocumentUri().takeIf { it.isNotBlank() }
            ?: error("Auto backup file is not set")
        val bytes = BackupImportService(context).exportBytes(scopes)

        writeToDocument(context.contentResolver, documentUri.toUri(), bytes)
    }

    fun displayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: FILE_NAME
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
