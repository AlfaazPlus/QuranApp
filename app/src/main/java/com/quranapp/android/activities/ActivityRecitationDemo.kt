package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.recitation.RecitationDemoScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.mediaplayer.RecitationController

class ActivityRecitationDemo : BaseActivity() {

    override fun getLayoutResource() = 0
    override fun getThemeId() = R.style.Theme_QuranApp_ComposeActivity

    override fun adjustSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isLight = isStatusBarLight()
        controller.isAppearanceLightStatusBars = isLight
        controller.isAppearanceLightNavigationBars = isLight
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val recitationController = RecitationController.getInstance(this)
        recitationController.connect()

        setContent {
            QuranAppTheme {
                RecitationDemoScreen(controller = recitationController)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RecitationController.getInstance(this).disconnect()
    }
}
