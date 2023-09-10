package com.quranapp.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.app.ThemeUtils
import com.quranapp.android.utils.exceptions.CustomExceptionHandler
import com.quranapp.android.utils.univ.FileUtils

class QuranApp : Application() {
    override fun attachBaseContext(base: Context) {
        initBeforeBaseAttach(base)
        super.attachBaseContext(base)
    }

    private fun initBeforeBaseAttach(base: Context) {
        FileUtils.appFilesDir = base.filesDir
        updateTheme(base)
    }

    private fun updateTheme(base: Context) {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeFromSP(base))
    }

    override fun onCreate() {
        super.onCreate()
        DownloadSourceUtils.resetDownloadSourceBaseUrl(this)
        NotificationUtils.createNotificationChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        // Handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler(this))
    }
}
