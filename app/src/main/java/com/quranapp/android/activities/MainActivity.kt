package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.MainScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.app.AppActions.checkForCrashLogs
import com.quranapp.android.utils.app.AppActions.checkForResourcesVersions
import com.quranapp.android.utils.app.AppActions.scheduleActions
import com.quranapp.android.utils.app.UpdateManager
import com.quranapp.android.utils.sharedPrefs.SPAppActions.getRequireOnboarding
import com.quranapp.android.views.reader.updateAllVotdWidgets

class MainActivity : BaseActivity() {
    private var mUpdateManager: UpdateManager? = null

    override fun getLayoutResource() = 0

    override fun onPause() {
        mUpdateManager?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mUpdateManager?.onResume()
    }

    override fun initCreate(savedInstanceState: Bundle?) {


        mUpdateManager = UpdateManager(this)
        mUpdateManager!!.refreshAppUpdatesJson()

        if (mUpdateManager!!.check4CriticalUpdate()) {
            return
        }

        if (this.isOnboardingRequired) {
            initOnboarding()
            return
        }

        super.initCreate(savedInstanceState)
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        if (this.isOnboardingRequired) {
            return
        }

        enableEdgeToEdge()

        initActions()
        updateAllVotdWidgets(this)

        setContent {
            QuranAppTheme {
                MainScreen()
            }
        }
    }


    private fun initActions() {
        checkForResourcesVersions(this)
        scheduleActions(this)
        checkForCrashLogs(this)
    }


    private val isOnboardingRequired get() = getRequireOnboarding(this)

    private fun initOnboarding() {
        launchActivity(ActivityOnboarding::class.java)
        finish()
    }
}
