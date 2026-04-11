package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.settings.SettingsScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import kotlinx.coroutines.flow.MutableStateFlow

class ActivitySettings : BaseActivity() {
    val intentFlow = MutableStateFlow<Pair<Intent?, Boolean>>(Pair(null, false))

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        intentFlow.value = Pair(intent, false)

        setContent {
            val currentIntentData by intentFlow.collectAsState()

            QuranAppTheme {
                SettingsScreen(currentIntentData.first, currentIntentData.second)
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        intentFlow.value = Pair(intent, true)
    }
}
