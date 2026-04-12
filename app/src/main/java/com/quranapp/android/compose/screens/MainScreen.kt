package com.quranapp.android.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.components.MainAppBar
import com.quranapp.android.compose.components.MainBottomNavigationBar
import com.quranapp.android.compose.components.mainBottomNavigationOuterHeight
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT_DP
import com.quranapp.android.compose.components.player.RecitationPlayerSheet

@Composable
fun MainScreen() {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { MainAppBar() },
            bottomBar = {
                MainBottomNavigationBar()
            }
        ) { paddingValues ->
            Box(
                Modifier
                    .padding(paddingValues)
                    .padding(bottom = MINI_PLAYER_HEIGHT_DP.dp)
            ) {
                HomeScreen()
            }
        }

        RecitationPlayerSheet(
            collapsedBottomInset = mainBottomNavigationOuterHeight(),
        )
    }
}