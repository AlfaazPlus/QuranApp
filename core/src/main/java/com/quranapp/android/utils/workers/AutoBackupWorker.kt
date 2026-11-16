package com.quranapp.android.utils.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quranapp.android.backup.AutoBackup
import com.quranapp.android.backup.BackupPreferences
import com.quranapp.android.utils.Log

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!BackupPreferences.isEnabled()) return Result.success()

        return try {
            AutoBackup.writeNow(applicationContext)
            Result.success()
        } catch (error: Exception) {
            Log.saveError(error, "autoBackup")
            Result.failure()
        }
    }
}
