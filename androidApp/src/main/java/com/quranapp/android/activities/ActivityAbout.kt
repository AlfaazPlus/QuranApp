package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.screens.AboutScreen
import com.quranapp.android.theme.QuranAppTheme

class ActivityAbout : BaseActivity() {
    override fun shouldInflateAsynchronously() = false

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                AboutScreen()
            }
        }
    }
}
