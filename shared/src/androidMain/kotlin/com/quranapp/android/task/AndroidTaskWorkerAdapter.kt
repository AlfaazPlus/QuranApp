package com.quranapp.android.task

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.platform.notification.NotificationChannels
import com.quranapp.android.shared.R
import kotlinx.coroutines.CoroutineScope

/**
 * Standard WorkManager [CoroutineWorker] that bridges execution to [TaskWorker].
 *
 * Instantiated automatically by WorkManager's default factory (requires no custom WorkerFactory).
 */
internal class AndroidTaskWorkerAdapter(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val className = inputData.getString(TaskWorkerProvider.KEY_WORKER_CLASS)
            ?: return failureResult("Missing target worker class name")

        val worker = TaskWorkerProvider.create(className)
            ?: return failureResult("Failed to instantiate worker: $className")

        val userParams = inputData.toTaskParams()
        val scope = CoroutineScope(kotlinx.coroutines.currentCoroutineContext())
        val context = AndroidTaskContext(userParams, scope)

        return when (val result = worker.doWork(context)) {
            is TaskResult.Success -> Result.success(result.output.toWorkData())

            is TaskResult.Failure -> {
                val outputData = result.output.toWorkData().mergeErrorMessage(result.message)
                if (result.retry) Result.retry() else Result.failure(outputData)
            }

            is TaskResult.Cancelled -> Result.failure(
                workDataOf(TaskProgressKeys.KEY_ERROR_MESSAGE to "Task cancelled"),
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val defaultInfo = ForegroundTaskInfo(
            notificationId = id.hashCode(),
            title = "Working…",
            indeterminate = true,
            showProgress = false,
        )

        return ForegroundNotificationProvider.instance?.buildForegroundInfo(defaultInfo) as? ForegroundInfo
            ?: defaultForegroundInfo(appContext, defaultInfo, id)
    }

    // ── Context implementation ────────────────────────────────────────────────

    private inner class AndroidTaskContext(
        override val params: TaskParams,
        override val coroutineScope: CoroutineScope,
    ) : TaskContext {

        override val isCancelled: Boolean
            get() = this@AndroidTaskWorkerAdapter.isStopped

        override suspend fun setProgress(progress: TaskProgress) {
            this@AndroidTaskWorkerAdapter.setProgress(buildProgressData(progress))
        }

        override suspend fun setForeground(info: ForegroundTaskInfo) {
            val foregroundInfo =
                ForegroundNotificationProvider.instance?.buildForegroundInfo(info) as? ForegroundInfo
                    ?: defaultForegroundInfo(appContext, info, id)

            this@AndroidTaskWorkerAdapter.setForeground(foregroundInfo)
        }
    }

    companion object {
        fun defaultForegroundInfo(
            appContext: Context,
            info: ForegroundTaskInfo,
            workId: java.util.UUID,
        ): ForegroundInfo {
            val channelId = NotificationChannels.CHANNEL_ID_DEFAULT
            val builder = NotificationCompat.Builder(appContext, channelId).apply {
                setSmallIcon(R.drawable.dr_logo)
                setContentTitle(info.title)
                info.body?.let { setContentText(it) }
                info.subText?.let { setSubText(it) }

                if (info.showProgress) {
                    val max = info.progressTotal.coerceAtLeast(1)
                    setProgress(max, info.progressCurrent.coerceIn(0, max), info.indeterminate)
                }

                setOngoing(true)
                setAutoCancel(false)
                addAction(
                    R.drawable.dr_icon_close,
                    "Cancel",
                    WorkManager.getInstance(appContext).createCancelPendingIntent(workId),
                )
            }

            val notification = builder.build()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    info.notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                ForegroundInfo(info.notificationId, notification)
            }
        }
    }
}

private fun failureResult(message: String): androidx.work.ListenableWorker.Result =
    androidx.work.ListenableWorker.Result.failure(
        workDataOf(TaskProgressKeys.KEY_ERROR_MESSAGE to message),
    )

private fun Data.mergeErrorMessage(message: String?): Data {
    if (message == null) return this
    val builder = Data.Builder()
    builder.putAll(this)
    builder.putString(TaskProgressKeys.KEY_ERROR_MESSAGE, message)
    return builder.build()
}

internal fun buildProgressData(progress: TaskProgress): Data =
    workDataOf(
        TaskProgressKeys.KEY_PROGRESS_CURRENT to progress.current,
        TaskProgressKeys.KEY_PROGRESS_TOTAL to progress.total,
        TaskProgressKeys.KEY_INDETERMINATE to progress.indeterminate,
        TaskProgressKeys.KEY_PROGRESS_MESSAGE to (progress.message ?: ""),
    )
