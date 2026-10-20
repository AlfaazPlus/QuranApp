package com.quranapp.android.compose.components.library

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityCollections
import com.quranapp.android.compose.components.collections.CreateUpdateCollectionSheet
import com.quranapp.android.compose.components.collections.collectionColor
import com.quranapp.android.compose.components.collections.collectionIcon
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.user.CollectionWithCount
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.CollectionsViewModel

@Composable
fun LibrarySectionCollections() {
    val viewModel = viewModel<CollectionsViewModel>()
    val collections by viewModel.recentCollections.collectAsStateWithLifecycle()

    var showCreateCollectionSheet by remember { mutableStateOf(false) }

    CreateUpdateCollectionSheet(
        showCreateCollectionSheet,
        onClose = { showCreateCollectionSheet = false },
    )

    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(
            iconRes = R.drawable.ic_library,
            title = stringResource(R.string.my_collections),
            headerRightContent = if (collections.isNotEmpty()) {
                {
                    SectionHeaderViewAll {
                        context.startActivity(Intent(context, ActivityCollections::class.java))
                    }
                }
            } else null,
        )

        when {
            collections.isEmpty() -> {
                CollectionsEmptyState(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    showCreateCollectionSheet = true
                }
            }

            else -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(shapes.medium)
                        .border(1.dp, colorScheme.outlineVariant.alpha(0.3f), shapes.medium),
                    color = colorScheme.surfaceContainer.alpha(0.5f),
                ) {
                    Column() {
                        collections.forEachIndexed { index, item ->
                            CollectionCard(
                                item = item,
                            ) {
                                context.startActivity(
                                    Intent(
                                        context,
                                        ActivityCollections::class.java
                                    ).apply {
                                        putExtra(Keys.COLLECTION_ID, item.collection.id)
                                    }
                                )
                            }

                            if (index < collections.lastIndex) {
                                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CollectionsEmptyState(
    modifier: Modifier = Modifier,
    onCreateCollection: () -> Unit,
) {
    Column(
        modifier = modifier
            .border(1.dp, colorScheme.outlineVariant.alpha(0.6f), shapes.medium)
            .padding(20.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.no_collections),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        OutlinedButton(
            onClick = onCreateCollection,
            border = BorderStroke(
                width = 1.dp,
                color = colorScheme.primary,
            ),
        ) {
            Text(
                text = stringResource(R.string.create_collection),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}


@Composable
private fun CollectionCard(
    item: CollectionWithCount,
    onClick: () -> Unit,
) {
    val collection = item.collection
    val color = collectionColor(collection.color)

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = color,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            collectionIcon(collection.icon_key)
                        ),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = pluralStringResource(
                        R.plurals.verse_count,
                        item.verseCount,
                        item.verseCount,
                    ),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.onSurfaceVariant.alpha(0.65f)
            )
        }
    }
}
