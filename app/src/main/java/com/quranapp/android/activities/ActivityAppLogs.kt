package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.settings.AppLogsScreen
import com.quranapp.android.compose.theme.QuranAppTheme

class ActivityAppLogs : BaseActivity() {
    override fun getLayoutResource(): Int {
        return 0
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContent {
            QuranAppTheme {
                AppLogsScreen()
            }
        }
    }

}
