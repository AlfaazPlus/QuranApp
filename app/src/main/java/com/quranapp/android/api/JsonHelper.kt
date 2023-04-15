package com.quranapp.android.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object JsonHelper {
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        explicitNulls = false
    }
}
