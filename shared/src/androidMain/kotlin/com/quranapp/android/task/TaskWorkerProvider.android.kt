package com.quranapp.android.task

import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap

private val constructorCache = ConcurrentHashMap<String, Constructor<out TaskWorker>>()

actual fun createDefaultTaskWorker(className: String): TaskWorker? {
    return try {
        val constructor = constructorCache.getOrPut(className) {
            @Suppress("UNCHECKED_CAST")
            val clazz = Class.forName(className) as Class<out TaskWorker>
            clazz.getDeclaredConstructor().apply { isAccessible = true }
        }
        constructor.newInstance()
    } catch (_: Throwable) {
        null
    }
}
