package com.quranapp.android.backup

import com.quranapp.android.platform.Platform
import com.quranapp.android.platform.files.FileUnavailableException
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.task.ExistingPeriodicTaskPolicy
import com.quranapp.android.task.TaskConstraints
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskSchedule
import com.quranapp.android.utils.workers.AutoBackupWorker
import okio.buffer
import kotlin.time.Duration.Companion.days

object AutoBackup {
    const val FILE_NAME = "quranapp-backup.json.gz"
    private const val TASK_NAME = "auto_backup"
    private const val MIME_TYPE = "application/gzip"

    private val scopes = mapOf(
        ExportKeys.BOOKMARKS to true,
        ExportKeys.COLLECTIONS to true,
        ExportKeys.SETTINGS to true,
    )

    private val externalFiles get() = Platform.externalFiles

    fun schedule(restart: Boolean = false) {
        val request = TaskRequest(
            workerClass = AutoBackupWorker::class,
            constraints = TaskConstraints.NONE,
            schedule = TaskSchedule.Periodic(1.days)
        )

        Platform.system.taskManager.enqueuePeriodic(
            TASK_NAME,
            if (restart) ExistingPeriodicTaskPolicy.UPDATE else ExistingPeriodicTaskPolicy.KEEP,
            request
        )
    }

    fun cancel() {
        Platform.system.taskManager.cancelUnique(TASK_NAME)
    }

    suspend fun ensureAvailable() {
        if (!BackupPreferences.isEnabled()) return

        val location = BackupPreferences.readDocumentPath().takeIf { it.isNotBlank() } ?: return
        if (displayName(location) == null) forgetMissing(location)
    }

    suspend fun writeNow(): Boolean {
        ensureAvailable()
        if (!BackupPreferences.isEnabled()) return false

        val location = BackupPreferences.readDocumentPath()
        val bytes = BackupImportService().exportBytes(scopes)

        return try {
            externalFiles.sink(location).buffer().use { it.write(bytes) }
            true
        } catch (_: FileUnavailableException) {
            forgetMissing(location)
            false
        }
    }

    fun displayName(location: String): String? =
        externalFiles.displayName(location)?.ifBlank { FILE_NAME }

    fun openFile(location: String): Boolean = externalFiles.view(location, MIME_TYPE)

    private suspend fun forgetMissing(location: String) {
        if (BackupPreferences.readDocumentPath() != location) return

        cancel()
        BackupPreferences.clearMissingFile()
        externalFiles.release(location)
    }
}
