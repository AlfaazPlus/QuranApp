package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.peacedesign.android.utils.DrawableUtils
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.AboutScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.extensions.isRTL
import com.quranapp.android.widgets.IconedTextView

class ActivityAbout : BaseActivity() {
    override fun shouldInflateAsynchronously() = false

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuranAppTheme {
                    AboutScreen()
                }
            }
        })
    }
}
