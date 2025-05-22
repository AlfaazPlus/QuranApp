package com.quranapp.android.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JsonHelper {
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        explicitNulls = false
    }
}

fun JsonObject?.safeInt(key: String): Int? {
    if (this == null) return null

    try {
        return this[key]?.jsonPrimitive?.intOrNull
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeInt(key: String, default: Int): Int {
    if (this == null) return default

    try {
        return this[key]?.jsonPrimitive?.intOrNull ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeString(key: String): String? {
    if (this == null) return null

    try {
        return this[key]?.jsonPrimitive?.contentOrNull
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeString(key: String, default: String): String {
    if (this == null) return default

    try {
        return this[key]?.jsonPrimitive?.contentOrNull ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeBoolean(key: String): Boolean? {
    if (this == null) return null

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toBoolean()
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeBoolean(key: String, default: Boolean): Boolean {
    if (this == null) return default

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeLong(key: String): Long? {
    if (this == null) return null

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toLong()
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeLong(key: String, default: Long): Long {
    if (this == null) return default

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toLong() ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeDouble(key: String): Double? {
    if (this == null) return null

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toDouble()
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeDouble(key: String, default: Double): Double {
    if (this == null) return default

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toDouble() ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeFloat(key: String): Float? {
    if (this == null) return null

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toFloat()
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeFloat(key: String, default: Float): Float {
    if (this == null) return default

    try {
        return this[key]?.jsonPrimitive?.contentOrNull?.toFloat() ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeJsonObject(key: String): JsonObject? {
    if (this == null) return null

    try {
        return this[key]?.jsonObject
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeJsonObject(key: String, default: JsonObject): JsonObject {
    if (this == null) return default

    try {
        return this[key]?.jsonObject ?: default
    } catch (e: Exception) {
        return default
    }
}

fun JsonObject?.safeJsonArray(key: String): JsonArray? {
    if (this == null) return null

    try {
        return this[key]?.jsonArray
    } catch (e: Exception) {
        return null
    }
}

fun JsonObject?.safeJsonArray(key: String, default: JsonArray): JsonArray {
    if (this == null) return default

    try {
        return this[key]?.jsonArray ?: default
    } catch (e: Exception) {
        return default
    }
}