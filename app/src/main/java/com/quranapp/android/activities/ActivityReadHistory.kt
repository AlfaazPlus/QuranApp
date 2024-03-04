package com.quranapp.android.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.quranapp.android.ui.screens.ReadHistoryScreen
import com.quranapp.android.ui.theme.QuranAppTheme

class ActivityReadHistory : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranAppTheme{
                ReadHistoryScreen()
            }
        }
    }
}