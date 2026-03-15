package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.reader.ReaderIndexScreen
import com.quranapp.android.compose.theme.QuranAppTheme

class ActivityReaderIndexPage : BaseActivity() {

    override fun getLayoutResource(): Int {
        return 0
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuranAppTheme {
                    ReaderIndexScreen(
                    )
                }
            }
        })
    }

}
