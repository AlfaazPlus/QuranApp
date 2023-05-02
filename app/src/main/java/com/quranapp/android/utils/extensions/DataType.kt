package com.quranapp.android.utils.extensions

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper

/**
 * Cast a class null safely to a specific type.
 * */
inline fun <reified T> Any?.safeCastTo(block: T.() -> Unit) {
    if (this is T) {
        block()
    }
}

/**
 * Cast a class null safely to a map.
 * */
fun Any?.safeCastToMap(block: Map<String, Any>.() -> Unit) {
    safeCastTo(block)
}

/**
 * Cast a class to specific type unchecked, result may be null.
 * */
inline fun <reified T> Any?.unsafeCastTo(): T? {
    if (this is T) {
        return this
    }
    return null
}

/**
 * Cast a class specific type unchecked, result may be null.
 * */
fun Any?.unsafeCastToMap(): Map<String, Any>? {
    return unsafeCastTo<Map<String, Any>>()
}

/**
 * Cast a class to specific type or default if null.
 * */
inline fun <reified T> Any?.castOrDefault(default: T): T {
    if (this is T) {
        return this
    }
    return default
}

/**
 * Since traditional [SharedPreferences.Editor] does not have a putDouble method, therefore here is the extension function.
 * The double stored by this method must be retrieved using [getDouble].
 * It converts the [Double] into its [Long] equivalent using [Double.doubleToRawLongBits][java.lang.Double.doubleToRawLongBits]
 * and stores it as Long.
 * */
fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))

/**
 * Since traditional [SharedPreferences.Editor] does not have a getDouble method, therefore here is the extension function.
 * The double being retrieved by this method must have been stored using [putDouble], which stored the double as its long equivalent.
 * It converts the [Long] into its original [Double] equivalent using [Double.longBitsToDouble][java.lang.Double.longBitsToDouble].
 * */
fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

fun String?.throwIfNullOrEmpty(): String {
    if (isNullOrEmpty()) {
        throw Exception()
    }
    return this
}

fun Any?.isBooleanTrue(): Boolean {
    if (this is Boolean) {
        return this
    }
    return false
}

fun Number?.throwIfNullOrNotPositive(): Number {
    if (this == null || this == 0) {
        throw Exception()
    }
    return this
}

inline fun <reified T> T?.orMinusOne(): T where T : Number {
    return this ?: (-1 as T)
}

fun String?.toIntOrMinusOne(): Int {
    return try {
        this?.toInt() ?: -1
    } catch (e: java.lang.Exception) {
        -1
    }
}

fun CharSequence.isOnlyLetters() = all { it.isLetter() }

inline fun Any?.ifNull(action: () -> Unit) {
    if (this == null) {
        action()
    }
}

inline fun runOnTimeout(crossinline block: () -> Unit, timeoutMillis: Long) {
    Handler(Looper.getMainLooper()).postDelayed({
        block()
    }, timeoutMillis)
}

// interval

fun runOnInterval(block: () -> Unit, intervalMillis: Long, runImmediately: Boolean = false): Handler {
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({
        block()
        runOnInterval(block, intervalMillis)
    }, intervalMillis)

    if (runImmediately) {
        block()
    }

    return handler
}
