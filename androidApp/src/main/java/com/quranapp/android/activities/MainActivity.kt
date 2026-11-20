package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.homepage.AppUpdateRequiredDialog
import com.quranapp.android.screens.main.MainScreen
import com.quranapp.android.theme.QuranAppTheme
import com.quranapp.android.utils.app.AppActions.scheduleActions
import com.quranapp.android.utils.app.UpdateManager
import com.quranapp.android.preferences.AppActionsPreferences
import com.quranapp.android.widgets.updateAllVotdWidgets

class MainActivity : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onResume() {
        super.onResume()

        updateAllVotdWidgets(this)
    }

    override fun initCreate(savedInstanceState: Bundle?) {
        if (UpdateManager.getInstance(this).check4CriticalUpdate()) {
            setContent {
                QuranAppTheme {
                    AppUpdateRequiredDialog()
                }
            }
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

        setContent {
            QuranAppTheme {
                MainScreen()
            }
        }
    }


    private fun initActions() {
        scheduleActions(this)
    }


    private val isOnboardingRequired get() = AppActionsPreferences.getRequireOnboarding()

    private fun initOnboarding() {
        startActivity(Intent(this, ActivityOnboarding::class.java))
        finish()
    }
}
