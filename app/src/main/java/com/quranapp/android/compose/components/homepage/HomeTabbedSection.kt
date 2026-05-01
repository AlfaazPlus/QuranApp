package com.quranapp.android.compose.components.homepage

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.quranapp.android.R
import com.quranapp.android.compose.components.HomePremiumBannerContainer
import com.quranapp.android.compose.components.VerseOfTheDay
import com.quranapp.android.compose.components.VotdContent
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.recommended.Recommendation
import com.quranapp.android.utils.recommended.Recommended

@Composable
fun HomeTabbedSection() {
    val context = LocalContext.current
    var recommendations by remember { mutableStateOf(emptyList<Recommendation>()) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        recommendations = Recommended.getRecommendations(context)
    }

    if (recommendations.isEmpty()) {
        VerseOfTheDay()
        return
    }

    HomePremiumBannerContainer {
        Column {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {}) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            stringResource(R.string.labelRecommended), style = typography.labelLarge
                        )
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            stringResource(R.string.strTitleVOTD), style = typography.labelLarge
                        )
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(
                color = Color.White.alpha(0.15f)
            )

            if (selectedTabIndex == 0) {
                HomeSectionRecommended(recommendations)
            } else {
                ReaderProvider {
                    VotdContent(header = {})
                }
            }
        }
    }
}
