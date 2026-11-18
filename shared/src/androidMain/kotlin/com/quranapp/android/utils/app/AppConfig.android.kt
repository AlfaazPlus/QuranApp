package com.quranapp.android.utils.app

import com.quranapp.android.shared.BuildConfig

actual val IS_DEBUG = BuildConfig.DEBUG

actual val APP_VERSION_CODE = BuildConfig.VERSION_CODE
actual val APP_VERSION_NAME =
    if (IS_DEBUG) "${BuildConfig.VERSION_NAME}-debug" else BuildConfig.VERSION_NAME
