package com.quranapp.android.task

import kotlinx.coroutines.CoroutineScope

/**
 * Functional contract for a background task worker.
 *
 * All execution logic lives inside [doWork].
 *
 * Implementations should provide a public zero-argument constructor so the platform runtime
 * can instantiate them automatically without manual registration.
 *
 * ### Example
 * ```kotlin
 * class SyncBookmarksWorker : TaskWorker {
 *     override suspend fun doWork(context: TaskContext): TaskResult {
 *         val userId = context.params.getString("user_id") ?: return TaskResult.Failure("Missing user_id")
 *
 *         context.setProgress(TaskProgress.percent(10, "Connecting…"))
 *         if (context.isCancelled) return TaskResult.Cancelled
 *
 *         // Do work...
 *         return TaskResult.Success()
 *     }
 * }
 * ```
 */
fun interface TaskWorker {
    /**
     * Executes the background task.
     *
     * @param context Provides access to input [TaskContext.params], cancellation status [TaskContext.isCancelled],
     *                the task's [TaskContext.coroutineScope], and suspend helpers for [TaskContext.setProgress]
     *                and [TaskContext.setForeground].
     * @return [TaskResult.Success], [TaskResult.Failure], or [TaskResult.Cancelled].
     */
    suspend fun doWork(context: TaskContext): TaskResult
}

/**
 * Execution environment supplied to [TaskWorker.doWork].
 */
interface TaskContext {
    /**
     * Input parameters passed when enqueuing the task via [TaskRequest.params].
     */
    val params: TaskParams

    /**
     * Coroutine scope bound to the lifetime of this task execution.
     * Cancelled automatically when the platform cancels or stops the task.
     */
    val coroutineScope: CoroutineScope

    /**
     * `true` when the platform or user has requested cancellation.
     * Check this in tight loops or non-suspending operations.
     */
    val isCancelled: Boolean

    /**
     * Emits a progress update observable via [TaskManager.progressOf].
     */
    suspend fun setProgress(progress: TaskProgress)

    /**
     * Promotes this task to a foreground service with an ongoing notification.
     *
     * Should be called early in [TaskWorker.doWork] when [TaskRequest.requiresForeground] is true.
     * On platforms without foreground services (e.g. iOS), this is a safe no-op.
     */
    suspend fun setForeground(info: ForegroundTaskInfo)
}

/**
 * Final outcome returned by [TaskWorker.doWork].
 */
sealed class TaskResult {

    /** The worker completed successfully. Optionally carries output data. */
    data class Success(val output: TaskParams = TaskParams.EMPTY) : TaskResult()

    /**
     * The worker encountered an unrecoverable or retryable error.
     *
     * @param message Human-readable error description.
     * @param retry   When true, the platform retries according to [TaskBackoffPolicy] in [TaskRequest].
     * @param output  Any partial output or diagnostic details.
     */
    data class Failure(
        val message: String? = null,
        val retry: Boolean = false,
        val output: TaskParams = TaskParams.EMPTY,
    ) : TaskResult()

    /** The worker detected cancellation and stopped cleanly. */
    data object Cancelled : TaskResult()
}
