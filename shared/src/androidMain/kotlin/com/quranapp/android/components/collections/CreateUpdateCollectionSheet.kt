package com.quranapp.android.components.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.TextInput
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.circle_minus
import com.quranapp.android.generated.resources.collection_description_hint
import com.quranapp.android.generated.resources.collection_name
import com.quranapp.android.generated.resources.collection_name_hint
import com.quranapp.android.generated.resources.create
import com.quranapp.android.generated.resources.create_collection
import com.quranapp.android.generated.resources.description
import com.quranapp.android.generated.resources.ic_check
import com.quranapp.android.generated.resources.select_color
import com.quranapp.android.generated.resources.select_icon
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelUpdate
import com.quranapp.android.generated.resources.update_collection
import com.quranapp.android.viewmodel.CollectionsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val COLOR_PRESETS = listOf(
    "#000000",
    "#2D2F3A",
    "#2F3B3B",
    "#003F2F",
    "#3B1C32",
    "#3D1E6D",
    "#0f4d8f",
    "#7a422c",
    "#5e2e5e",
    "#75692e",
    "#6e2b2b",
)

private val ICON_PRESETS = listOf(
    "reading",
    "growth",
    "day",
    "devotion",
    "saved",
    "reflection",
    "night",
    "protection",
    "favorite",
    "learning",
    "recitation",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUpdateCollectionSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
    collectionToUpdate: CollectionEntity? = null,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        title = stringResource(
            if (collectionToUpdate != null) Res.string.update_collection
            else Res.string.create_collection
        ),
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
        sheetGesturesEnabled = false,
    ) {
        Content(
            collectionToUpdate = collectionToUpdate,
            onClose = onClose,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.Content(
    collectionToUpdate: CollectionEntity?,
    onClose: () -> Unit,
) {
    val viewModel = appViewModel<CollectionsViewModel>()
    var name by remember(collectionToUpdate) { mutableStateOf(collectionToUpdate?.name ?: "") }
    var description by remember(collectionToUpdate) {
        mutableStateOf(
            collectionToUpdate?.description ?: ""
        )
    }
    var color by remember(collectionToUpdate) { mutableStateOf(collectionToUpdate?.color) }
    var iconKey by remember(collectionToUpdate) { mutableStateOf(collectionToUpdate?.icon_key) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextInput(
            value = name,
            onValueChange = { name = it },
            maxLines = 1,
            label = stringResource(Res.string.collection_name),
            placeholder = stringResource(Res.string.collection_name_hint),
        )
        TextInput(
            value = description,
            onValueChange = { description = it },
            maxLines = 5,
            minLines = 5,
            label = stringResource(Res.string.description),
            placeholder = stringResource(Res.string.collection_description_hint),
        )
        ColorSelector(
            selectedColor = color,
            onColorSelected = { color = it },
            onReset = { color = null },
        )
        IconSelector(
            selectedIcon = iconKey,
            onIconSelected = { iconKey = it },
            onReset = { iconKey = null },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(stringResource(Res.string.strLabelCancel))
        }
        Button(
            modifier = Modifier.weight(1f),
            enabled = name.isNotBlank(),
            onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) return@Button
                val descriptionValue = description.trim().ifBlank { null }
                if (collectionToUpdate != null) {
                    viewModel.updateCollection(
                        collectionToUpdate.copy(
                            name = trimmedName,
                            description = descriptionValue,
                            icon_key = iconKey,
                            color = color,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                } else {
                    viewModel.addCollection(
                        CollectionEntity(
                            name = trimmedName,
                            description = descriptionValue,
                            icon_key = iconKey,
                            color = color,
                        )
                    )
                }
                onClose()
            },
        ) {
            Text(
                stringResource(
                    if (collectionToUpdate != null) Res.string.strLabelUpdate
                    else Res.string.create
                )
            )
        }
    }
}

@Composable
private fun IconSelector(
    selectedIcon: String?,
    onIconSelected: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.select_icon),
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
        )

        val cells = listOf<String?>(null) + ICON_PRESETS
        cells.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    val selected = selectedIcon == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .border(
                                1.dp,
                                if (selected) colorScheme.primary else Color.Transparent,
                                MaterialTheme.shapes.medium,
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                if (key == null) onReset() else onIconSelected(key)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(collectionIcon(key)),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                repeat(6 - row.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorSelector(
    selectedColor: String?,
    onColorSelected: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.select_color),
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
        )

        val cells = listOf<String?>(null) + COLOR_PRESETS
        cells.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { colorHex ->
                    if (colorHex == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .border(
                                    1.dp,
                                    if (selectedColor == null) colorScheme.primary else Color.Transparent,
                                    MaterialTheme.shapes.medium,
                                )
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onReset() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.circle_minus),
                                contentDescription = null,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onColorSelected(colorHex) }
                                .background(collectionColor(colorHex)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_check),
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
