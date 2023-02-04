package com.quranapp.android.api

import kotlinx.serialization.json.Json

object JsonHelper {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}