package com.quranapp.android.compose.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.TextFieldValue
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
        label = inputLabel(label),
        placeholder = inputPlaceholder(placeholder),
        shape = inputShape(),
        colors = inputColors(bgColor),
        enabled = enabled,
    )
}

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
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
        label = inputLabel(label),
        placeholder = inputPlaceholder(placeholder),
        shape = inputShape(),
        colors = inputColors(bgColor),
        enabled = enabled,
    )
}

@Composable
private fun inputLabel(label: String?): (@Composable () -> Unit)? {
    if (label == null) return null
    return { Text(text = label) }
}

@Composable
private fun inputPlaceholder(placeholder: String?): (@Composable () -> Unit)? {
    if (placeholder == null) return null
    return { Text(text = placeholder) }
}

@Composable
private fun inputShape(): Shape = MaterialTheme.shapes.medium

@Composable
private fun inputColors(bgColor: Color): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = bgColor,
        focusedContainerColor = bgColor,
        unfocusedLabelColor = colorScheme.onBackground.alpha(0.6f),
        unfocusedBorderColor = colorScheme.outline.alpha(0.3f),
        focusedBorderColor = colorScheme.primary,
    )
}
