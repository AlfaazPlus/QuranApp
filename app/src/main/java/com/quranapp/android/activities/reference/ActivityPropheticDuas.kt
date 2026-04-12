package com.quranapp.android.activities.reference

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.reference.PropheticDuasScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.univ.Keys

class ActivityPropheticDuas : BaseActivity() {
    override fun getLayoutResource() = 0


    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        val title = intent.getStringExtra(Keys.KEY_EXTRA_TITLE)

        setContent {
            QuranAppTheme {
                PropheticDuasScreen(title = title)
            }
        }
    }
}
