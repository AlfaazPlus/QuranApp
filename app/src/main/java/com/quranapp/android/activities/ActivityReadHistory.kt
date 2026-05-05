package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.ReadHistoryScreen
import com.quranapp.android.compose.theme.QuranAppTheme

class ActivityReadHistory : BaseActivity() {
    override fun getLayoutResource() = 0


    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                ReadHistoryScreen()
            }
        }
    }
}
