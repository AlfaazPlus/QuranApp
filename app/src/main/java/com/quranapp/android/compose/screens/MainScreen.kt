package com.quranapp.android.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quranapp.android.compose.components.MainAppBar
import com.quranapp.android.compose.components.MainBottomNavigationBar

@Composable
fun MainScreen() {
    Scaffold(
        topBar = { MainAppBar() },
        bottomBar = {
            MainBottomNavigationBar()
        }
    ) {
        Box(
            Modifier.padding(it)
        ) {
            HomeScreen()
        }
    }
}