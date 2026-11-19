package com.quranapp.android.task

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import androidx.work.ExistingPeriodicWorkPolicy as WmPeriodicPolicy
import androidx.work.ExistingWorkPolicy as WmOneTimePolicy
import com.quranapp.android.task.ExistingPeriodicTaskPolicy as AppPeriodicPolicy
import com.quranapp.android.task.ExistingTaskPolicy as AppOneTimePolicy

/**
 * [TaskManager] implementation backed by Android [WorkManager].
 */
class AndroidTaskManager(private val appContext: Context) : TaskManager {

    private val wm: WorkManager get() = WorkManager.getInstance(appContext)

    // ── Scheduling ────────────────────────────────────────────────────────────

    override fun enqueue(request: TaskRequest): TaskId {
        val wmRequest = request.toOneTimeRequest()
        wm.enqueue(wmRequest)
        return TaskId(wmRequest.id.toString())
    }

    override fun enqueueUnique(
        uniqueName: String,
        policy: AppOneTimePolicy,
        request: TaskRequest,
    ): TaskId {
        val wmRequest = request.toOneTimeRequest()
        wm.enqueueUniqueWork(uniqueName, policy.toWm(), wmRequest)
        return TaskId(wmRequest.id.toString())
    }

    override fun enqueuePeriodic(
        uniqueName: String,
        policy: AppPeriodicPolicy,
        request: TaskRequest,
    ): TaskId {
        val schedule = request.schedule as? TaskSchedule.Periodic
            ?: error("enqueuePeriodic requires TaskSchedule.Periodic, got ${request.schedule}")

        val wmRequest = request.toPeriodicRequest(schedule)
        wm.enqueueUniquePeriodicWork(uniqueName, policy.toWm(), wmRequest)
        return TaskId(wmRequest.id.toString())
    }

    // ── Cancellation ──────────────────────────────────────────────────────────

    override fun cancel(id: TaskId) {
        val uuid = runCatching { java.util.UUID.fromString(id.value) }.getOrNull() ?: return
        wm.cancelWorkById(uuid)
    }

    override fun cancelByTag(tag: String) {
        wm.cancelAllWorkByTag(tag)
    }

    override fun cancelUnique(uniqueName: String) {
        wm.cancelUniqueWork(uniqueName)
    }

    override fun cancelAll() {
        wm.cancelAllWork()
    }

    // ── Observation ───────────────────────────────────────────────────────────

    override fun stateOf(id: TaskId): Flow<TaskState> {
        val uuid = runCatching { java.util.UUID.fromString(id.value) }.getOrNull()
            ?: return kotlinx.coroutines.flow.flowOf(TaskState.Unknown)

        return wm.getWorkInfoByIdFlow(uuid)
            .map { info -> info?.let { fromWorkInfo(it) } ?: TaskState.Unknown }
            .distinctUntilChanged()
    }

    override fun stateByTag(tag: String): Flow<List<TaskState>> {
        return wm.getWorkInfosByTagFlow(tag)
            .map { infos -> infos.map { fromWorkInfo(it) } }
            .distinctUntilChanged()
    }

    override fun progressOf(id: TaskId): Flow<TaskProgress?> {
        val uuid = runCatching { java.util.UUID.fromString(id.value) }.getOrNull()
            ?: return kotlinx.coroutines.flow.flowOf(null)

        return wm.getWorkInfoByIdFlow(uuid)
            .map { info ->
                if (info?.state == WorkInfo.State.RUNNING) {
                    extractProgress(info.progress)
                } else {
                    null
                }
            }
            .distinctUntilChanged()
    }

    override fun isRunningByTag(tag: String): Flow<Boolean> {
        return wm.getWorkInfosByTagFlow(tag)
            .map { infos ->
                infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            }
            .distinctUntilChanged()
    }

    // ── Internal builders ─────────────────────────────────────────────────────

    private fun TaskRequest.toOneTimeRequest(): OneTimeWorkRequest {
        val inputData = buildInputData()
        val constraints = constraints.toWmConstraints()

        return OneTimeWorkRequestBuilder<AndroidTaskWorkerAdapter>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .apply {
                tags.forEach { addTag(it) }

                if (backoffDelay > Duration.ZERO) {
                    setBackoffCriteria(
                        backoffPolicy.toWm(),
                        backoffDelay.inWholeMilliseconds,
                        TimeUnit.MILLISECONDS,
                    )
                }

                if (initialDelay > Duration.ZERO) {
                    setInitialDelay(initialDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                }
            }
            .build()
    }

    private fun TaskRequest.toPeriodicRequest(schedule: TaskSchedule.Periodic): PeriodicWorkRequest {
        val inputData = buildInputData()
        val constraints = constraints.toWmConstraints()
        val intervalMs = schedule.interval.inWholeMilliseconds
        val flexMs = schedule.flexInterval?.inWholeMilliseconds

        val builder = if (flexMs != null) {
            PeriodicWorkRequestBuilder<AndroidTaskWorkerAdapter>(
                intervalMs,
                TimeUnit.MILLISECONDS,
                flexMs,
                TimeUnit.MILLISECONDS,
            )
        } else {
            PeriodicWorkRequestBuilder<AndroidTaskWorkerAdapter>(intervalMs, TimeUnit.MILLISECONDS)
        }

        return builder
            .setInputData(inputData)
            .setConstraints(constraints)
            .apply { tags.forEach { addTag(it) } }
            .build()
    }

    private fun TaskRequest.buildInputData(): Data {
        val className = workerClass.qualifiedName
            ?: workerClass.simpleName
            ?: error("Cannot determine class name for $workerClass")

        return params.toWorkData(workerClassName = className)
    }

    private fun fromWorkInfo(info: WorkInfo): TaskState {
        return when (info.state) {
            WorkInfo.State.ENQUEUED -> TaskState.Enqueued
            WorkInfo.State.RUNNING -> TaskState.Running(progress = extractProgress(info.progress))
            WorkInfo.State.SUCCEEDED -> TaskState.Succeeded(output = info.outputData.toTaskParams())
            WorkInfo.State.FAILED -> TaskState.Failed(
                message = info.outputData.getString(TaskProgressKeys.KEY_ERROR_MESSAGE),
                output = info.outputData.toTaskParams(),
            )

            WorkInfo.State.CANCELLED -> TaskState.Cancelled
            WorkInfo.State.BLOCKED -> TaskState.Enqueued // treated as waiting
        }
    }

    private fun extractProgress(data: androidx.work.Data): TaskProgress? {
        if (data.keyValueMap.isEmpty()) return null

        val indeterminate = data.getBoolean(TaskProgressKeys.KEY_INDETERMINATE, false)
        val current = data.getInt(TaskProgressKeys.KEY_PROGRESS_CURRENT, 0)
        val total = data.getInt(TaskProgressKeys.KEY_PROGRESS_TOTAL, 100)
        val message = data.getString(TaskProgressKeys.KEY_PROGRESS_MESSAGE)
        val extras = data.toTaskParams()

        return TaskProgress(
            current = current,
            total = total,
            indeterminate = indeterminate,
            message = message,
            extras = extras,
        )
    }
}

private fun AppOneTimePolicy.toWm(): WmOneTimePolicy = when (this) {
    AppOneTimePolicy.KEEP -> WmOneTimePolicy.KEEP
    AppOneTimePolicy.REPLACE -> WmOneTimePolicy.REPLACE
    AppOneTimePolicy.APPEND -> WmOneTimePolicy.APPEND
    AppOneTimePolicy.APPEND_OR_REPLACE -> WmOneTimePolicy.APPEND_OR_REPLACE
}

private fun AppPeriodicPolicy.toWm(): WmPeriodicPolicy = when (this) {
    AppPeriodicPolicy.KEEP -> WmPeriodicPolicy.KEEP
    AppPeriodicPolicy.REPLACE -> WmPeriodicPolicy.CANCEL_AND_REENQUEUE
    AppPeriodicPolicy.CANCEL_AND_REENQUEUE -> WmPeriodicPolicy.CANCEL_AND_REENQUEUE
    AppPeriodicPolicy.UPDATE -> WmPeriodicPolicy.UPDATE
}

private fun TaskBackoffPolicy.toWm(): androidx.work.BackoffPolicy = when (this) {
    TaskBackoffPolicy.LINEAR -> androidx.work.BackoffPolicy.LINEAR
    TaskBackoffPolicy.EXPONENTIAL -> androidx.work.BackoffPolicy.EXPONENTIAL
}

private fun TaskConstraints.toWmConstraints(): Constraints {
    return Constraints.Builder()
        .setRequiredNetworkType(if (networkRequired) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
        .setRequiresCharging(chargingRequired)
        .setRequiresStorageNotLow(storageNotLow)
        .setRequiresBatteryNotLow(batteryNotLow)
        .apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                setRequiresDeviceIdle(deviceIdle)
            }
        }
        .build()
}

/**
 * Convert [TaskParams] to a WorkManager [Data] object.
 *
 * If [workerClassName] is provided, it is inserted in the same pass, avoiding extra Data copies.
 */
fun TaskParams.toWorkData(workerClassName: String? = null): Data =
    Data.Builder().apply {
        workerClassName?.let {
            putString(TaskWorkerProvider.KEY_WORKER_CLASS, it)
        }
        putAll(map)
    }.build()

/**
 * Convert a WorkManager [Data] object back to a [TaskParams] instance.
 * Excludes internal task framework keys.
 */
fun Data.toTaskParams(): TaskParams =
    TaskParams.Builder().apply {
        keyValueMap.forEach { (key, value) ->
            if (key != TaskWorkerProvider.KEY_WORKER_CLASS) {
                when (value) {
                    is String, is Int, is Long, is Float, is Double,
                    is Boolean, is IntArray, is LongArray, is FloatArray,
                    is DoubleArray, is BooleanArray -> put(key, value)

                    is Array<*> ->
                        if (value.javaClass.componentType == String::class.java)
                            put(key, value)
                }
            }
        }
    }.build()

/** Keys used to serialise [TaskProgress] into / out of WorkManager [Data]. */
object TaskProgressKeys {
    const val KEY_PROGRESS_CURRENT = "__progress_current__"
    const val KEY_PROGRESS_TOTAL = "__progress_total__"
    const val KEY_INDETERMINATE = "__progress_indeterminate__"
    const val KEY_PROGRESS_MESSAGE = "__progress_message__"
    const val KEY_ERROR_MESSAGE = "__error_message__"
}
