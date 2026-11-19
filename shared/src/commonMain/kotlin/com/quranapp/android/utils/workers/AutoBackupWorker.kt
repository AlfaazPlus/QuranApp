package com.quranapp.android.utils.workers

import com.quranapp.android.backup.AutoBackup
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.app.Logger

class AutoBackupWorker() : TaskWorker {
    override suspend fun doWork(context: TaskContext): TaskResult {
        if (!BackupPreferences.isEnabled()) return TaskResult.Success()

        return try {
            AutoBackup.writeNow()
            TaskResult.Success()
        } catch (error: Exception) {
            Logger.saveError(error, "autoBackup")
            TaskResult.Failure()
        }
    }
}
