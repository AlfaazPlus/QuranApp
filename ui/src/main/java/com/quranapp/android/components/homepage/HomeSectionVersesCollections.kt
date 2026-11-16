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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.quran.ExclusiveVersesScreenKind
import com.quranapp.android.components.quran.QuranicTopicsStart
import com.quranapp.android.core.R
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.alpha
import com.quranapp.android.theme.grayscaleColorFilter
import com.quranapp.android.theme.observeIsMonoPalette
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.miscellenous.Keys

@Composable
fun HomeSectionVersesCollections() {
    val context = LocalContext.current

    val entries = remember {
        listOf(
            VersesCollectionCard(
                R.string.strTitleFeaturedDuas,
                R.drawable.dr_icon_rabbana
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.EXCLUSIVE_VERSES).putExtra(
                        Keys.KEY_EXCLUSIVE_VERSES_KIND,
                        ExclusiveVersesScreenKind.Dua.name,
                    )
                )
            },
            VersesCollectionCard(
                R.string.titleSolutionVerses,
                R.drawable.dr_icon_read_quran
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.EXCLUSIVE_VERSES).putExtra(
                        Keys.KEY_EXCLUSIVE_VERSES_KIND,
                        ExclusiveVersesScreenKind.Solution.name,
                    )
                )
            },
            VersesCollectionCard(
                R.string.titleEtiquetteVerses,
                R.drawable.icon_veiled_muslim,
                tint = false
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.EXCLUSIVE_VERSES).putExtra(
                        Keys.KEY_EXCLUSIVE_VERSES_KIND,
                        ExclusiveVersesScreenKind.Etiquette.name,
                    )
                )
            },
            VersesCollectionCard(
                R.string.strTitleMajorSins,
                R.drawable.icon_major_sins,
                tint = false
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.EXCLUSIVE_VERSES).putExtra(
                        Keys.KEY_EXCLUSIVE_VERSES_KIND,
                        ExclusiveVersesScreenKind.MajorSins.name,
                    )
                )
            },
            VersesCollectionCard(
                R.string.strTitleFeaturedProphets,
                R.drawable.prophets,
                tint = false
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.PROPHETS)
                )
            },
            VersesCollectionCard(
                R.string.quran_and_science,
                R.drawable.ic_science_embryology,
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.QURAN_SCIENCE)
                )
            },
            VersesCollectionCard(
                R.string.ontologyExplorer,
                R.drawable.hierarchy,
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.QURANIC_TOPICS).putExtra(
                        Keys.KEY_QURANIC_TOPICS_SCREEN_TYPE,
                        QuranicTopicsStart.Ontology.name
                    )
                )
            },
            VersesCollectionCard(
                R.string.thematicTopics,
                R.drawable.dr_icon_read_quran,
            ) {
                context.startActivity(
                    AppDestinations.intent(context, AppDestinations.QURANIC_TOPICS).putExtra(
                        Keys.KEY_QURANIC_TOPICS_SCREEN_TYPE,
                        QuranicTopicsStart.Thematic.name
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
    val titleRes: Int,
    val iconRes: Int,
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
