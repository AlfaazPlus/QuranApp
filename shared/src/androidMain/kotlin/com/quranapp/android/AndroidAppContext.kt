package com.quranapp.android

import android.content.Context

object AndroidAppContext {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context {
        check(::appContext.isInitialized) {
            "AndroidAppContext is not initialized"
        }

        return appContext
    }
}
