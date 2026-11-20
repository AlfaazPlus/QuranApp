package com.quranapp.android.task

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic entry point for scheduling, cancelling, and observing background tasks.
 *
 * ## Scheduling
 *
 * - [enqueue] – fire-and-forget one-time work.
 * - [enqueueUnique] – one-time work deduplicated by name.
 * - [enqueuePeriodic] – recurring work deduplicated by name.
 *
 * ## Cancellation
 *
 * - [cancel] by [TaskId] (exact match).
 * - [cancelByTag] – cancel all tasks that share a tag.
 * - [cancelUnique] – cancel unique-named work.
 * - [cancelAll] – cancel every pending / running task.
 *
 * ## Observation
 *
 * All observation APIs return cold [Flow]s.  Collect from an appropriate coroutine scope tied
 * to your UI lifecycle to avoid leaks.
 *
 * - [stateOf] – state of a single task by id.
 * - [stateByTag] – states of all tasks matching a tag.
 * - [progressOf] – latest progress snapshot for a task.
 * - [isRunningByTag] – convenience boolean flow for a tag.
 */
interface TaskManager {

    // ── Scheduling ────────────────────────────────────────────────────────────

    /**
     * Enqueue a one-time task.  Returns immediately with a [TaskId] that can be used for
     * cancellation and observation.
     */
    fun enqueue(request: TaskRequest): TaskId

    /**
     * Enqueue a one-time task identified by [uniqueName].
     *
     * When a task with the same name already exists, [policy] determines what happens.
     */
    fun enqueueUnique(
        uniqueName: String,
        policy: ExistingTaskPolicy,
        request: TaskRequest,
    ): TaskId

    /**
     * Enqueue a periodic task identified by [uniqueName].
     *
     * [request.schedule] must be [TaskSchedule.Periodic].
     */
    fun enqueuePeriodic(
        uniqueName: String,
        policy: ExistingPeriodicTaskPolicy,
        request: TaskRequest,
    ): TaskId

    // ── Cancellation ──────────────────────────────────────────────────────────

    /** Cancel the task with the given [TaskId]. No-op if it no longer exists. */
    fun cancel(id: TaskId)

    /** Cancel all tasks that have [tag] in their tag set. */
    fun cancelByTag(tag: String)

    /** Cancel unique-named work (one-time or periodic). */
    fun cancelUnique(uniqueName: String)

    /** Cancel every pending and running task managed by this instance. */
    fun cancelAll()

    // ── Observation ───────────────────────────────────────────────────────────

    /**
     * Returns a [Flow] that emits the current [TaskState] for the given [id] and updates
     * whenever the state changes.  Emits [TaskState.Unknown] if the id is not found.
     */
    fun stateOf(id: TaskId): Flow<TaskState>

    /**
     * Returns a [Flow] that emits a list of [TaskInfo]s for every task matching [tag].
     * The list is empty when no matching tasks exist.
     */
    fun infosByTag(tag: String): Flow<List<TaskInfo>>

    /**
     * Returns a [Flow] of the latest [TaskProgress] for [id], or null before any progress
     * is reported or after the task finishes.
     */
    fun progressOf(id: TaskId): Flow<TaskProgress?>

    /**
     * Returns a [Flow<Boolean>] that is `true` while at least one task with [tag] is running
     * or enqueued, and `false` otherwise.
     */
    fun isRunningByTag(tag: String): Flow<Boolean>
}

/**
 * Opaque identifier for a background task returned by [TaskManager.enqueue] and siblings.
 *
 * On Android this wraps the WorkManager UUID string.  Other platforms are free to use any
 * stable string format.
 */
@JvmInline
value class TaskId(val value: String) {
    override fun toString(): String = value
}

/** One scheduled task matching a tag query. */
data class TaskInfo(
    val id: TaskId,
    val tags: Set<String>,
    val state: TaskState,
)

/** How to handle a [TaskManager.enqueuePeriodic] call when periodic work with the same name already exists. */
enum class ExistingPeriodicTaskPolicy {
    /** Keep the existing periodic work; ignore the new request. */
    KEEP,

    /** Cancel the existing periodic work and enqueue the new one. */
    REPLACE,

    /** Cancel and re-enqueue the existing work (resets timer). */
    CANCEL_AND_REENQUEUE,

    /**
     * Update the interval/constraints of the existing work without resetting its run timer.
     * Falls back to [CANCEL_AND_REENQUEUE] on platforms that don't support in-place updates.
     */
    UPDATE,
}

/** How to handle a [TaskManager.enqueueUnique] call when work with the same name already exists. */
enum class ExistingTaskPolicy {
    /** Keep existing work; ignore the new request. */
    KEEP,

    /** Cancel existing work and enqueue the new request. */
    REPLACE,

    /** Append the new request to run after the existing chain finishes. */
    APPEND,

    /** Append if existing work is pending; replace if it failed or was cancelled. */
    APPEND_OR_REPLACE,
}

/**
 * Observable lifecycle state of a background task.
 *
 * Consumers collect this via [TaskManager.stateOf] or [TaskManager.stateByTag].
 */
sealed class TaskState {

    /** Work has been accepted by the scheduler but has not started yet. */
    data object Enqueued : TaskState()

    /** Work is actively executing. Progress may be non-null. */
    data class Running(val progress: TaskProgress? = null) : TaskState()

    /** Work finished successfully. */
    data class Succeeded(val output: TaskParams = TaskParams.EMPTY) : TaskState()

    /**
     * Work finished with an error.
     *
     * @param message Error description, if available.
     * @param output  Any partial / diagnostic output from the worker.
     */
    data class Failed(
        val message: String? = null,
        val output: TaskParams = TaskParams.EMPTY,
    ) : TaskState()

    /** Work was cancelled before or during execution. */
    data object Cancelled : TaskState()

    /**
     * No matching work was found, or the platform does not support state observation for this id.
     * Treat as a terminal non-error state.
     */
    data object Unknown : TaskState()

    // ── Convenience predicates ────────────────────────────────────────────────

    val isFinished: Boolean
        get() = this is Succeeded || this is Failed || this is Cancelled

    val isRunning: Boolean
        get() = this is Running

    val isEnqueued: Boolean
        get() = this is Enqueued
}

/**
 * Progress snapshot emitted by a running worker via [TaskContext.setProgress].
 *
 * [current] and [total] are the raw units (bytes, chapters, items). [percentage] is derived
 * from them for UI.
 *
 * @param current Current units of work completed. Ignored when [indeterminate] is true.
 * @param total   Total units of work. Ignored when [indeterminate] is true.
 * @param indeterminate True when progress cannot be measured (spinner-style).
 * @param message Optional human-readable progress label (e.g. "12 MB / 48 MB").
 * @param extras  Additional platform-agnostic data attached by the worker.
 */
data class TaskProgress(
    val current: Long = 0,
    val total: Long = 100,
    val indeterminate: Boolean = false,
    val message: String? = null,
    val extras: TaskParams = TaskParams.EMPTY,
) {
    /** Progress as a 0–100 percentage, or null when [indeterminate]. */
    val percentage: Int?
        get() = if (indeterminate || total <= 0) null
        else (current.coerceIn(0, total) * 100 / total).toInt()

    companion object {
        val INDETERMINATE = TaskProgress(indeterminate = true)

        fun percent(pct: Int, message: String? = null): TaskProgress =
            TaskProgress(current = pct.toLong(), total = 100, message = message)

        fun of(current: Int, total: Int, message: String? = null): TaskProgress =
            of(current.toLong(), total.toLong(), message = message)

        fun of(
            current: Long,
            total: Long,
            indeterminate: Boolean = false,
            message: String? = null,
        ): TaskProgress = TaskProgress(
            current = current,
            total = total,
            indeterminate = indeterminate || total <= 0L,
            message = message,
        )
    }
}
