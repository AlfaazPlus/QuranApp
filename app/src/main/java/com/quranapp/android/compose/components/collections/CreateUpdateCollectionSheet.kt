package com.quranapp.android.compose.components.collections

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.TextInput
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.viewModels.CollectionsViewModel
import java.util.Date

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
    "mic",
    "book",
    "heart",
    "sun",
    "leaf",
    "mosque",
)

fun collectionColor(hex: String?): Color {
    val default = Color(80, 84, 71)
    if (hex.isNullOrBlank()) return default

    return try {
        Color(hex.toColorInt())
    } catch (_: IllegalArgumentException) {
        default
    }
}

fun collectionIcon(iconKey: String?): Int {
    return when (iconKey) {
        "mic" -> R.drawable.col_mic
        "book" -> R.drawable.col_book
        "heart" -> R.drawable.col_heart
        "sun" -> R.drawable.col_sun
        "leaf" -> R.drawable.col_leaf
        "mosque" -> R.drawable.col_mosque
        else -> R.drawable.col_book
    }
}


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
            if (collectionToUpdate != null) R.string.update_collection
            else R.string.create_collection
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
    val viewModel = viewModel<CollectionsViewModel>()
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
            label = stringResource(R.string.collection_name),
            placeholder = stringResource(R.string.collection_name_hint),
        )
        TextInput(
            value = description,
            onValueChange = { description = it },
            maxLines = 5,
            minLines = 5,
            label = stringResource(R.string.description),
            placeholder = stringResource(R.string.collection_description_hint),
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
            Text(stringResource(R.string.strLabelCancel))
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
                            updatedAt = Date(),
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
                    if (collectionToUpdate != null) R.string.strLabelUpdate
                    else R.string.create
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
            stringResource(R.string.select_icon),
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
                            painter = painterResource(
                                if (key == null) R.drawable.circle_minus else collectionIcon(key)
                            ),
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
            stringResource(R.string.select_color),
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
                                painter = painterResource(R.drawable.circle_minus),
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
                                .background(Color(colorHex.toColorInt())),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
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
