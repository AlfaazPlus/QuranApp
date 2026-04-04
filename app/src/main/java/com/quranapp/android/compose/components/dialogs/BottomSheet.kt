package com.quranapp.android.compose.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.theme.alpha


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetBare(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = dragHandle,
    ) {
        header?.invoke()
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    icon: Int? = null,
    title: String? = null,
    headerArrangement: Arrangement.Horizontal = Arrangement.Center,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable () -> Unit,
) {
    BottomSheetBare(
        isOpen = isOpen,
        onDismiss = onDismiss,
        dragHandle = dragHandle,
        header = {
            BottomSheetHeader(icon, title, headerArrangement, dragHandle != null)
        },
        content = content,
    )
}

@Composable
fun BottomSheetHeader(
    icon: Int? = null,
    title: String? = null,
    headerArrangement: Arrangement.Horizontal = Arrangement.Center,
    hasDragHandle: Boolean,
) {
    if (icon != null || title != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (hasDragHandle) 0.dp else 16.dp,
                    bottom = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalArrangement = headerArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}