package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.MainScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.app.AppActions.checkForCrashLogs
import com.quranapp.android.utils.app.AppActions.scheduleActions
import com.quranapp.android.utils.app.UpdateManager
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.views.reader.updateAllVotdWidgets

class MainActivity : BaseActivity() {
    private var mUpdateManager: UpdateManager? = null

    override fun getLayoutResource() = 0

    override fun initCreate(savedInstanceState: Bundle?) {
        if (UpdateManager.getInstance(this).check4CriticalUpdate()) {
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

        initActions()
        updateAllVotdWidgets(this)

        setContent {
            QuranAppTheme {
                MainScreen()
            }
        }
    }


    private fun initActions() {
        scheduleActions(this)
        checkForCrashLogs(this)
    }


    private val isOnboardingRequired get() = SPAppActions.getRequireOnboarding(this)

    private fun initOnboarding() {
        startActivity(Intent(this, ActivityOnboarding::class.java))
        finish()
    }
}
