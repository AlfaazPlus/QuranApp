/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 2/2/2023.
 * All rights reserved.
 */

package com.quranapp.android.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig

fun createApiHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 60_000
        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
    }
}

object ApiConfig {
    const val GH_PROXY_ROOT = "https://gh-proxy.alfaazplus.com/"
    const val JS_DELIVR_ROOT = "https://cdn.jsdelivr.net/gh/"
    const val GH_RAW_ROOT = "https://raw.githubusercontent.com/"
    const val GH_PROXY_BASE_URL = "${GH_PROXY_ROOT}AlfaazPlus/QuranApp/master/"
    const val JS_DELIVR_BASE_URL = "${JS_DELIVR_ROOT}AlfaazPlus/QuranApp@latest/"
    const val GH_RAW_BASE_URL = "${GH_RAW_ROOT}AlfaazPlus/QuranApp/master/"
    const val GITHUB_REPOSITORY_URL = "https://github.com/AlfaazPlus/QuranApp"
    const val GITHUB_ISSUES_BUG_REPORT_URL =
        "https://github.com/AlfaazPlus/QuranApp/issues/new?template=bug_report.yml"
    const val GITHUB_ISSUES_VERSE_REPORT_URL =
        "https://github.com/AlfaazPlus/QuranApp/issues/new?template=verse_report.yml"

    const val QURAN_API_ROOT_URL = "https://api.quran.com/"
    const val ALFAAZPLUS_API_ROOT_URL = "https://api.alfaazplus.com"
}
