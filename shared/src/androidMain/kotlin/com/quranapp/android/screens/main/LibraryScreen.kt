package com.quranapp.android.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.IndexMenuButton
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.components.library.LibrarySectionBookmarks
import com.quranapp.android.components.library.LibrarySectionCollections
import com.quranapp.android.components.library.LibrarySectionReadHistory
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_search
import com.quranapp.android.generated.resources.library
import com.quranapp.android.generated.resources.strLabelNavSearch
import com.quranapp.android.utils.app.AppDestinations
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val HOME_HISTORY_LIMIT = 10

@Composable
fun LibraryScreen() {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            topBar = {
                AppBar(
                    title = stringResource(Res.string.library),
                    showNavigationIcon = false,
                    actions = {
                        SimpleTooltip(text = stringResource(Res.string.strLabelNavSearch)) {
                            IconButton(
                                modifier = Modifier.size(40.dp),
                                onClick = {
                                    context.startActivity(
                                        AppDestinations.intent(context, AppDestinations.SEARCH)
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        Res.drawable.dr_icon_search
                                    ),
                                    contentDescription = stringResource(Res.string.strLabelNavSearch),
                                    tint = colorScheme.onSurface
                                )
                            }
                        }

                        IndexMenuButton()
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 192.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                LibrarySectionReadHistory()
                LibrarySectionBookmarks()
                LibrarySectionCollections()
            }
        }
    }
}
