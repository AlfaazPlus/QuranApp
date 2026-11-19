package com.quranapp.android.task

import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Full specification for a unit of background work.
 *
 * @param workerClass   [KClass] of the [TaskWorker] implementation to run.
 * @param params        Input key-value parameters passed to [TaskContext.params].
 * @param constraints   Hardware / connectivity requirements. Defaults to [TaskConstraints.NONE].
 * @param schedule      Whether to run once or periodically. Defaults to [TaskSchedule.OneTime].
 * @param tags          Arbitrary string tags for group cancellation and state queries.
 * @param backoffPolicy Retry strategy when worker returns [TaskResult.Failure] with `retry=true`.
 * @param backoffDelay  Initial delay before the first retry attempt.
 * @param requiresForeground When true, the task will be promoted to a foreground service on Android.
 * @param initialDelay  Optional initial delay before the first run.
 */
data class TaskRequest(
    val workerClass: KClass<out TaskWorker>,
    val params: TaskParams = TaskParams.EMPTY,
    val constraints: TaskConstraints = TaskConstraints.NETWORK,
    var schedule: TaskSchedule = TaskSchedule.OneTime,
    val tags: Set<String> = emptySet(),
    var backoffPolicy: TaskBackoffPolicy = TaskBackoffPolicy.EXPONENTIAL,
    var backoffDelay: Duration = 30_000.milliseconds,
    var requiresForeground: Boolean = false,
    var initialDelay: Duration = Duration.ZERO,
)

/**
 * Typed key-value bag used for worker input data and progress extras.
 *
 * Values must be one of: String, Int, Long, Float, Double, Boolean,
 * or their respective Array equivalents.  Other types are rejected at build time.
 *
 * Keep the total serialised size small (WorkManager caps at 10 KB on Android).
 */
class TaskParams private constructor(
    @PublishedApi internal val map: Map<String, Any>
) {
    inline fun <reified T> get(key: String): T? = map[key] as? T

    inline fun <reified T> get(key: String, default: T): T =
        get<T>(key) ?: default

    fun int(key: String, default: Int = 0) =
        (map[key] as? Number)?.toInt() ?: default

    fun long(key: String, default: Long = 0L) =
        (map[key] as? Number)?.toLong() ?: default

    fun float(key: String, default: Float = 0f) =
        (map[key] as? Number)?.toFloat() ?: default

    fun double(key: String, default: Double = 0.0) =
        (map[key] as? Number)?.toDouble() ?: default

    fun contains(key: String) = key in map
    fun isEmpty() = map.isEmpty()

    class Builder {
        private val map = mutableMapOf<String, Any>()

        fun put(key: String, value: Any) = apply {
            require(
                value is String || value is Int || value is Long ||
                        value is Float || value is Double || value is Boolean ||
                        value is Array<*> && value.isArrayOf<String>() || value is IntArray ||
                        value is LongArray || value is FloatArray ||
                        value is DoubleArray || value is BooleanArray
            ) {
                "Unsupported value type: ${value::class}"
            }

            map[key] = value
        }

        fun putNullableString(key: String, value: String?) = apply {
            value?.let { put(key, it) }
        }

        fun build() = TaskParams(map.toMap())
    }

    companion object {
        val EMPTY = TaskParams(emptyMap())

        fun of(vararg pairs: Pair<String, Any>) =
            Builder().apply { pairs.forEach { put(it.first, it.second) } }.build()
    }
}

/** Convenience DSL to build [TaskParams]. */
inline fun buildTaskParams(block: TaskParams.Builder.() -> Unit): TaskParams =
    TaskParams.Builder().apply(block).build()

/**
 * Hardware / connectivity prerequisites for a background task.
 *
 * Not all constraints are honoured on all platforms; each platform impl documents which ones it
 * supports.  Unconstrained tasks receive [NONE].
 */
data class TaskConstraints(
    /** Require an active network connection (any type). */
    val networkRequired: Boolean = false,
    /** Require the device to be charging. */
    val chargingRequired: Boolean = false,
    /** Require sufficient storage space. */
    val storageNotLow: Boolean = false,
    /** Require sufficient battery level. */
    val batteryNotLow: Boolean = false,
    /** Require the device to be idle (Doze). */
    val deviceIdle: Boolean = false,
) {
    companion object {
        val NONE = TaskConstraints()

        val NETWORK = TaskConstraints(networkRequired = true)

        val NETWORK_CHARGING = TaskConstraints(
            networkRequired = true,
            chargingRequired = true,
        )
    }
}

/**
 * Describes how often a task should run.
 */
sealed class TaskSchedule {

    /** Run exactly once, as soon as constraints are met. */
    data object OneTime : TaskSchedule()

    /**
     * Run repeatedly on the given interval.
     *
     * @param interval     Minimum period between executions. Must be ≥ 15 minutes on Android.
     * @param flexInterval Optional flex window at the end of [interval] in which the job may run
     *                     early. Android only; ignored on other platforms.
     */
    data class Periodic(
        val interval: Duration,
        val flexInterval: Duration? = null,
    ) : TaskSchedule() {
        init {
            require(interval >= 15.minutes) {
                "Periodic interval must be at least 15 minutes (platform minimum). Got: $interval"
            }
            if (flexInterval != null) {
                require(flexInterval > Duration.ZERO && flexInterval <= interval) {
                    "flexInterval must be > 0 and ≤ interval"
                }
            }
        }
    }
}

/** Retry back-off strategy when a worker returns [TaskResult.Failure] with `retry = true`. */
enum class TaskBackoffPolicy {
    /** Next attempt is scheduled after `delay * attemptCount`. */
    LINEAR,

    /** Next attempt is scheduled after `delay * 2^attemptCount`. */
    EXPONENTIAL,
}
