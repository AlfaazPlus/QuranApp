package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.screens.onboarding.OnboardingScreen
import com.quranapp.android.theme.QuranAppTheme

class ActivityOnboarding : BaseActivity() {

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                OnboardingScreen(onComplete = ::takeOff)
            }
        }
    }

    private fun takeOff() {
        launchMainActivity()
        finish()
    }
}
