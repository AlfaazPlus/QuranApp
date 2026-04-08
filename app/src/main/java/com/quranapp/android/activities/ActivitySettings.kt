package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.settings.SettingsScreen
import com.quranapp.android.compose.theme.QuranAppTheme

class ActivitySettings : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContent {
            QuranAppTheme {
                SettingsScreen(intent)
            }
        }
    }
}
