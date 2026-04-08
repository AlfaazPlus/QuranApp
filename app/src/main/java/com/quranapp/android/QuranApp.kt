package com.quranapp.android

import ThemeUtils
import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.bookmark.UserDataMigrationManager
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.exceptions.CustomExceptionHandler
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.viewModels.ReaderIndexViewModel

class QuranApp : Application() {
    override fun attachBaseContext(base: Context) {
        initBeforeBaseAttach(base)
        super.attachBaseContext(base)
    }

    private fun initBeforeBaseAttach(base: Context) {
        FileUtils.appFilesDir = base.filesDir
    }

    private fun updateTheme() {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeForDelegate())
    }

    override fun onCreate() {
        super.onCreate()
        DataStoreManager.init(this)
        DownloadSourceUtils.resetDownloadSourceBaseUrl()
        NotificationUtils.createNotificationChannels(this)
        updateTheme()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        // Handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler(this))
        ThemeUtils.migrateThemePreferences(this)
        ReaderIndexViewModel.migrateFavourites(this)
        ReaderPreferences.migrateFromLegacyIfNeeded(this)
        val migrationManager = UserDataMigrationManager(this)
        migrationManager.migrateBookmarksIfNeeded()
        migrationManager.migrateReadHistoryIfNeeded()
    }
}
