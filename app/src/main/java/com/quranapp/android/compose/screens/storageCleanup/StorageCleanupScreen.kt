package com.quranapp.android.compose.screens.storageCleanup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar

enum class StorageCleanupPane(val titleRes: Int) {
    Hub(R.string.titleStorageCleanup),
    Translations(R.string.strTitleTranslations),
    Recitations(R.string.strTitleRecitations),
    Scripts(R.string.strTitleScripts),
    Tafsir(R.string.strTitleTafsir),
}


@Composable
fun StorageCleanupScreen() {
    var pane by remember { mutableStateOf(StorageCleanupPane.Hub) }

    if (pane != StorageCleanupPane.Hub) {
        BackHandler {
            pane = StorageCleanupPane.Hub
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(pane.titleRes),
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = pane,
            label = "storageCleanupPane",
        ) { target ->
            when (target) {
                StorageCleanupPane.Hub -> {
                    StorageCleanupMainScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                        onOpenSection = { pane = it },
                    )
                }

                StorageCleanupPane.Translations -> {
                    StorageCleanupTranslationScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )
                }

                StorageCleanupPane.Recitations -> {
                    StorageCleanupRecitationScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )
                }

                StorageCleanupPane.Scripts -> {
                    StorageCleanupScriptsScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )
                }

                StorageCleanupPane.Tafsir -> {
                    StorageCleanupTafsirScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )
                }
            }
        }
    }
}
