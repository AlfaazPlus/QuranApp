package com.quranapp.android.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.quranapp.android.ui.screens.ReadHistoryScreen
import com.quranapp.android.ui.theme.QuranAppTheme

class ActivityReadHistory : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranAppTheme{
                ReadHistoryScreen()
            }
        }
    }
}