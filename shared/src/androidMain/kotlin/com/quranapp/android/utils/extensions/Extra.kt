package com.quranapp.android.utils.extensions

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.quranapp.android.api.JsonHelper
import java.io.Serializable

inline fun <reified T : Serializable> Bundle.serializableExtra(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Serializable> Intent.serializableExtra(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(
        key,
        T::class.java
    )

    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}

inline fun <reified T> Intent.putKotlinSerializableExtra(key: String, value: T) {
    putExtra(key, JsonHelper.json.encodeToString(value))
}

inline fun <reified T> Intent.kotlinSerializableExtra(key: String): T? =
    getStringExtra(key)?.let { JsonHelper.json.decodeFromString(it) }

inline fun <reified T : Parcelable> Bundle.parcelableExtra(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(
        key,
        T::class.java
    )

    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}
