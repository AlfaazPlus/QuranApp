package com.quranapp.android.compose.components.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import verticalFadingEdge

enum class AlertDialogActionStyle {
    Default,
    Primary,
    Danger,
}

data class AlertDialogAction(
    val text: String,
    val style: AlertDialogActionStyle = AlertDialogActionStyle.Default,
    val dismissOnClick: Boolean = true,
    val onClick: () -> Unit = {},
)

@Composable
internal fun AlertDialogAction.toTextButtonColors(): ButtonColors {
    val scheme = MaterialTheme.colorScheme

    val (container, content) = when (style) {
        AlertDialogActionStyle.Default -> scheme.surfaceVariant to scheme.onSurfaceVariant
        AlertDialogActionStyle.Primary -> scheme.primary to scheme.onPrimary
        AlertDialogActionStyle.Danger -> scheme.errorContainer to scheme.onPrimary
    }

    return ButtonDefaults.textButtonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = Color.Unspecified,
        disabledContentColor = Color.Unspecified,
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AlertDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    title: String,
    properties: DialogProperties = DialogProperties(
        windowTitle = title,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false
    ),
    actions: List<AlertDialogAction> = emptyList(),
    content: @Composable () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    if (!isOpen) return

    val configuration = LocalConfiguration.current
    val maxDialogWidth = remember(configuration.screenWidthDp) {
        minOf(610.dp, configuration.screenWidthDp.dp * 0.9f)
    }
    val maxDialogHeight = remember(configuration.screenHeightDp) {
        minOf(560.dp, configuration.screenHeightDp.dp * 0.9f)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = properties,
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .widthIn(min = 280.dp, max = maxDialogWidth)
                .heightIn(min = 72.dp, max = maxDialogHeight),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            contentColor = colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalFadingEdge(scrollState, color = colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(
                                horizontal = 20.dp,
                                vertical = 16.dp
                            )
                            .fillMaxWidth(),
                    ) {
                        content()
                    }
                }

                if (actions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        actions.forEach { action ->
                            TextButton(
                                modifier = Modifier.weight(1f),
                                colors = action.toTextButtonColors(),
                                onClick = {
                                    if (action.dismissOnClick) {
                                        onClose()
                                    }

                                    action.onClick()
                                },
                            ) {
                                Text(action.text)
                            }
                        }
                    }
                }
            }
        }
    }
}
