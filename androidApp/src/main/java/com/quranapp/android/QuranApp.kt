package com.quranapp.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.bookmark.UserDataMigrationManager
import com.quranapp.android.db.createDatabaseBuilders
import com.quranapp.android.locale.refreshAppLocale
import com.quranapp.android.platform.installAndroidFileSystem
import com.quranapp.android.platform.installAndroidPlatform
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.preferences.migrateLegacySharedPreferences
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.resolveThemeModeForDelegate
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.exceptions.CustomExceptionHandler
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.viewmodel.ReaderIndexViewModel
import com.quranapp.android.widgets.startRecitationPlayerWidgetObserver
import com.quranapp.android.widgets.startVotdWidgetPreferenceObserver
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class QuranApp : Application() {
    override fun attachBaseContext(base: Context) {
        initBeforeBaseAttach(base)
        super.attachBaseContext(base)
    }

    private fun initBeforeBaseAttach(base: Context) {
        installAndroidFileSystem(base)
    }

    private fun updateTheme() {
        val mode = runBlocking { DataStoreManager.readFirst(ThemeUtils.KEY_THEME_MODE) }
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeForDelegate(mode))
    }

    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.init(this)

        installAndroidPlatform(this)

        val builders = createDatabaseBuilders(applicationContext)
        runBlocking {
            DatabaseProvider.initialize(builders)
        }

        DataStoreManager.init(filesDir.toOkioPath())
        migrateLegacySharedPreferences(this)
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
        RecitationModelManager.migrateLegacyData()
        WbwAudioRepository.migrateLegacyData()
        ReaderIndexViewModel.migrateFavourites(this)
        UserDataMigrationManager(this).migrate()

        SearchIndexScheduler.scheduleTranslationSearchIndexIfNeeded(applicationContext)
    }
}
