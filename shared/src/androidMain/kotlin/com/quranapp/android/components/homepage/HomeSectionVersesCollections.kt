package com.quranapp.android.components.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_rabbana
import com.quranapp.android.generated.resources.dr_icon_read_quran
import com.quranapp.android.generated.resources.hierarchy
import com.quranapp.android.generated.resources.ic_science_embryology
import com.quranapp.android.generated.resources.icon_major_sins
import com.quranapp.android.generated.resources.icon_veiled_muslim
import com.quranapp.android.generated.resources.ontologyExplorer
import com.quranapp.android.generated.resources.prophets
import com.quranapp.android.generated.resources.quran_and_science
import com.quranapp.android.generated.resources.strTitleFeaturedDuas
import com.quranapp.android.generated.resources.strTitleFeaturedProphets
import com.quranapp.android.generated.resources.strTitleMajorSins
import com.quranapp.android.generated.resources.thematicTopics
import com.quranapp.android.generated.resources.titleEtiquetteVerses
import com.quranapp.android.generated.resources.titleSolutionVerses
import com.quranapp.android.models.ExclusiveVersesScreenKind
import com.quranapp.android.models.QuranicTopicsStart
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.alpha
import com.quranapp.android.theme.grayscaleColorFilter
import com.quranapp.android.theme.observeIsMonoPalette
import com.quranapp.android.utils.miscellenous.Keys
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeSectionVersesCollections() {
    val context = LocalContext.current

    val entries = remember {
        listOf(
            VersesCollectionCard(
                Res.string.strTitleFeaturedDuas,
                Res.drawable.dr_icon_rabbana
            ) {
                Platform.navigator.navigate(
                    AppRoute.Screen(
                        screen = AppRoutes.EXCLUSIVE_VERSES,
                        extras = mapOf(Keys.KEY_EXCLUSIVE_VERSES_KIND to ExclusiveVersesScreenKind.Dua.name)
                    )
                )
            },
            VersesCollectionCard(
                Res.string.titleSolutionVerses,
                Res.drawable.dr_icon_read_quran
            ) {
                Platform.navigator.navigate(
                    AppRoute.Screen(
                        screen = AppRoutes.EXCLUSIVE_VERSES,
                        extras = mapOf(Keys.KEY_EXCLUSIVE_VERSES_KIND to ExclusiveVersesScreenKind.Solution.name)
                    )
                )
            },
            VersesCollectionCard(
                Res.string.titleEtiquetteVerses,
                Res.drawable.icon_veiled_muslim,
                tint = false
            ) {
                Platform.navigator.navigate(
                    AppRoute.Screen(
                        screen = AppRoutes.EXCLUSIVE_VERSES,
                        extras = mapOf(Keys.KEY_EXCLUSIVE_VERSES_KIND to ExclusiveVersesScreenKind.Etiquette.name)
                    )
                )
            },
            VersesCollectionCard(
                Res.string.strTitleMajorSins,
                Res.drawable.icon_major_sins,
                tint = false
            ) {
                Platform.navigator.navigate(
                    AppRoute.Screen(
                        screen = AppRoutes.EXCLUSIVE_VERSES,
                        extras = mapOf(Keys.KEY_EXCLUSIVE_VERSES_KIND to ExclusiveVersesScreenKind.MajorSins.name)
                    )
                )
            },
            VersesCollectionCard(
                Res.string.strTitleFeaturedProphets,
                Res.drawable.prophets,
                tint = false
            ) {
                Platform.navigator.navigate(AppRoute.Screen(AppRoutes.PROPHETS))
            },
            VersesCollectionCard(
                Res.string.quran_and_science,
                Res.drawable.ic_science_embryology,
            ) {
                Platform.navigator.navigate(AppRoute.Screen(AppRoutes.QURAN_SCIENCE))
            },
            VersesCollectionCard(
                Res.string.ontologyExplorer,
                Res.drawable.hierarchy,
            ) {
                Platform.navigator.navigate(
                    AppRoute.Screen(
                        screen = AppRoutes.QURANIC_TOPICS,
                        extras = mapOf(Keys.KEY_QURANIC_TOPICS_SCREEN_TYPE to QuranicTopicsStart.Ontology.name)
                    )
                )
            },
            VersesCollectionCard(
                Res.string.thematicTopics,
                Res.drawable.dr_icon_read_quran,
            ) {
                Platform.navigator.navigate(
                    AppRoute.Screen(
                        screen = AppRoutes.QURANIC_TOPICS,
                        extras = mapOf(Keys.KEY_QURANIC_TOPICS_SCREEN_TYPE to QuranicTopicsStart.Thematic.name)
                    )
                )
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        entries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    CollectionCard(
                        entry = item,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class VersesCollectionCard(
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    val tint: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun CollectionCard(
    entry: VersesCollectionCard,
    modifier: Modifier = Modifier
) {
    val isMono = ThemeUtils.observeIsMonoPalette()
    val grayscale = remember { grayscaleColorFilter() }

    Card(
        onClick = entry.onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer.alpha(0.9f)
        ),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {

            Image(
                painter = painterResource(entry.iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                alpha = 0.9f,
                colorFilter = when {
                    entry.tint -> ColorFilter.tint(colorScheme.onSurface)
                    isMono -> grayscale
                    else -> null
                }
            )

            Text(
                text = stringResource(entry.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(end = 46.dp)
            )
        }
    }
}
