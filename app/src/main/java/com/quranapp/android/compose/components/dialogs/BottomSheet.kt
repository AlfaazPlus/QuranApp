package com.quranapp.android.compose.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.theme.alpha


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetBare(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        WindowInsets.safeDrawing.only(
            WindowInsetsSides.Bottom
        )
    },
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = if (sheetGesturesEnabled) dragHandle else null,
        contentWindowInsets = contentWindowInsets,
        sheetGesturesEnabled = sheetGesturesEnabled
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
    actions: (@Composable RowScope.() -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        WindowInsets.safeDrawing.only(
            WindowInsetsSides.Bottom
        )
    },
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dh = if (sheetGesturesEnabled) dragHandle else null
    BottomSheetBare(
        isOpen = isOpen,
        onDismiss = onDismiss,
        dragHandle = dh,
        header = {
            BottomSheetHeader(icon, title, headerArrangement, dh != null, actions)
        },
        contentWindowInsets = contentWindowInsets,
        sheetGesturesEnabled = sheetGesturesEnabled,
        content = content,
    )
}

@Composable
fun BottomSheetHeader(
    icon: Int? = null,
    title: String? = null,
    headerArrangement: Arrangement.Horizontal = Arrangement.Center,
    hasDragHandle: Boolean,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    if (icon != null || title != null || actions != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (hasDragHandle) 0.dp else 16.dp,
                    bottom = if (actions == null) 16.dp else 4.dp,
                    start = 16.dp,
                    end = if (actions == null) 16.dp else 8.dp,
                ),
            horizontalArrangement = if (actions == null) headerArrangement else Arrangement.Start,
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
                    maxLines = if (actions != null) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (actions != null) Modifier.weight(1f) else Modifier,
                )
            }

            actions?.invoke(this)
        }
    }
}
