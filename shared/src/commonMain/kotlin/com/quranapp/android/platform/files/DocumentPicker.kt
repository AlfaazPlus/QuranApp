package com.quranapp.android.platform.files

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that opens a document picker.
 * [onPicked] receives a location string, or null when cancelled.
 */
@Composable
expect fun rememberDocumentPicker(
    mimeTypes: List<String>,
    onPicked: (String?) -> Unit,
): () -> Unit
