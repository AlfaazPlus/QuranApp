package com.quranapp.android.compose.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.quranapp.android.compose.theme.alpha

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    bgColor: Color = colorScheme.background,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        minLines = minLines,
        label = if (label != null) {
            { Text(text = label) }
        } else null,
        placeholder = if (placeholder != null) {
            { Text(text = placeholder) }
        } else null,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = bgColor,
            focusedContainerColor = bgColor,
            unfocusedLabelColor = colorScheme.onBackground.alpha(0.6f),
            unfocusedBorderColor = colorScheme.outline.alpha(0.3f),
            focusedBorderColor = colorScheme.primary,
        ),
        enabled = enabled,
    )
}
