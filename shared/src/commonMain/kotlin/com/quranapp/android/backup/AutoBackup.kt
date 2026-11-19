package com.quranapp.android.backup

import android.content.ContentResolver
import android.content.Intent
import android.provider.OpenableColumns
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.task.ExistingPeriodicTaskPolicy
import com.quranapp.android.task.TaskConstraints
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskSchedule
import com.quranapp.android.utils.workers.AutoBackupWorker
import okio.Path
import okio.Path.Companion.toPath
import java.io.FileNotFoundException
import kotlin.time.Duration.Companion.days

object AutoBackup {
    const val FILE_NAME = "quranapp-backup.json.gz"
    private const val TASK_NAME = "auto_backup"
    private const val PERSISTABLE_URI_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    private val scopes = mapOf(
        ExportKeys.BOOKMARKS to true,
        ExportKeys.COLLECTIONS to true,
        ExportKeys.SETTINGS to true,
    )

    fun schedule(restart: Boolean = false) {
        val request = TaskRequest(
            workerClass = AutoBackupWorker::class,
            constraints = TaskConstraints.NONE,
            schedule = TaskSchedule.Periodic(1.days)
        )

        PlatformServices.system.taskManager.enqueuePeriodic(
            TASK_NAME,
            if (restart) ExistingPeriodicTaskPolicy.UPDATE else ExistingPeriodicTaskPolicy.KEEP,
            request
        )
    }

    fun cancel() {
        PlatformServices.system.taskManager.cancelUnique(TASK_NAME)
    }

    suspend fun ensureAvailable() {
        if (!BackupPreferences.isEnabled()) return

        val raw = BackupPreferences.readDocumentPath().takeIf { it.isNotBlank() } ?: return
        val path = raw.toPath()
        if (displayName(path) == null) forgetMissing(path)
    }

    suspend fun writeNow(): Boolean {
        ensureAvailable()
        if (!BackupPreferences.isEnabled()) return false

        val uri = BackupPreferences.readDocumentPath().toPath()
        val bytes = BackupImportService().exportBytes(scopes)

        return try {
            writeToDocument(context.contentResolver, uri, bytes)
            true
        } catch (error: FileNotFoundException) {
            forgetMissing(uri)
            false
        } catch (error: SecurityException) {
            forgetMissing(uri)
            false
        }
    }

    fun displayName(path: Path): String? {
        return try {
            val cursor = context.contentResolver.query(
                path,
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

    private suspend fun forgetMissing(path: Path) {
        if (BackupPreferences.readDocumentPath() != path.toString()) return

        cancel()
        BackupPreferences.clearMissingFile()

        runCatching {
            context.contentResolver.releasePersistableUriPermission(path, PERSISTABLE_URI_FLAGS)
        }
    }

    fun openFile(path: Path): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(path, "application/gzip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private fun writeToDocument(resolver: ContentResolver, path: Path, bytes: ByteArray) {
        val stream = try {
            resolver.openOutputStream(uri, "wt")
        } catch (_: IllegalArgumentException) {
            resolver.openOutputStream(uri)
        } ?: error("Could not open backup file")

        stream.use { it.write(bytes) }
    }
}
