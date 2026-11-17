package com.quranapp.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.bookmark.UserDataMigrationManager
import com.quranapp.android.locale.refreshAppLocale
import com.quranapp.android.platform.installAndroidFileSystem
import com.quranapp.android.platform.installAndroidPlatform
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.preferences.migrateFromLegacyIfNeeded
import com.quranapp.android.preferences.repairStoredPreferencesIfNeeded
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import okio.Path.Companion.toOkioPath
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.exceptions.CustomExceptionHandler
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.viewmodel.ReaderIndexViewModel
import com.quranapp.android.widgets.startRecitationPlayerWidgetObserver
import com.quranapp.android.widgets.startVotdWidgetPreferenceObserver
import com.quranapp.android.shared.BuildConfig as SharedBuildConfig
import com.quranapp.android.theme.resolveThemeModeForDelegate

class QuranApp : Application() {
    init {
        SharedBuildConfig.DEBUG = BuildConfig.DEBUG

        if (BuildConfig.DEBUG) {
            SharedBuildConfig.VERSION_NAME = "${SharedBuildConfig.VERSION_NAME}-debug"
        }
    }

    override fun attachBaseContext(base: Context) {
        initBeforeBaseAttach(base)
        super.attachBaseContext(base)
    }

    private fun initBeforeBaseAttach(base: Context) {
        installAndroidFileSystem(base)
    }

    private fun updateTheme() {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeForDelegate())
    }

    override fun onCreate() {
        super.onCreate()
        DatabaseProvider.install(this)
        installAndroidPlatform(this)
        DataStoreManager.init(filesDir.toOkioPath())
        ReaderPreferences.onTafsirSelected = { TafsirManager.setSavedTafsirKey(it) }
        refreshAppLocale(applicationContext)
        DownloadSourceUtils.resetDownloadSourceBaseUrl()
        NotificationUtils.createNotificationChannels(this)
        updateTheme()
        startVotdWidgetPreferenceObserver(this)
        startRecitationPlayerWidgetObserver(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        // Handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler(this))

        // Migrations
        ReaderPreferences.migrateFromLegacyIfNeeded(this)
        ReaderPreferences.repairStoredPreferencesIfNeeded(applicationContext)
        RecitationModelManager.migrateLegacyData()
        WbwAudioRepository.migrateLegacyData(applicationContext)
        ReaderIndexViewModel.migrateFavourites(this)
        UserDataMigrationManager(this).migrate()

        SearchIndexScheduler.scheduleTranslationSearchIndexIfNeeded(applicationContext)
    }
}
