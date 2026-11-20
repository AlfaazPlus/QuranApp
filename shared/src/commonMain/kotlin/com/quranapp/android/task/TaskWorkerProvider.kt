package com.quranapp.android.task

import kotlin.reflect.KClass

/**
 * Platform hook to create a [TaskWorker] from its fully-qualified class name.
 *
 * On Android / JVM: Instantiates using cached reflection (requires public zero-arg constructor).
 * On iOS: Returns null (workers are registered manually via [TaskWorkerProvider.register]).
 */
expect fun createDefaultTaskWorker(className: String): TaskWorker?

/**
 * Resolver for [TaskWorker] instances.
 *
 * Workers with a public zero-argument constructor work automatically on Android with **no setup**.
 * Custom factories can be registered for dependency injection or for non-JVM platforms like iOS.
 */
object TaskWorkerProvider {

    private val customFactories = mutableMapOf<String, () -> TaskWorker>()

    /** Internal key stored in platform task data to identify the target worker class. */
    const val KEY_WORKER_CLASS = "__worker_class__"

    /**
     * Register a custom factory for [workerClass].
     * Useful for dependency injection or platforms where reflection is unavailable.
     */
    @Synchronized
    fun register(workerClass: KClass<out TaskWorker>, factory: () -> TaskWorker) {
        val key = workerClass.qualifiedName ?: workerClass.simpleName ?: return
        customFactories[key] = factory
    }

    /**
     * Reified convenience overload for [register].
     */
    inline fun <reified T : TaskWorker> register(noinline factory: () -> T) {
        register(T::class, factory)
    }

    /**
     * Create a [TaskWorker] instance for [className].
     *
     * 1. Checks custom registered factories first (fast path).
     * 2. Falls back to platform default (cached reflection on Android).
     */
    @Synchronized
    fun create(className: String): TaskWorker? {
        customFactories[className]?.let { return it() }
        return createDefaultTaskWorker(className)
    }
}
