package com.quranapp.android.platform.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDocumentPicker(
    mimeTypes: List<String>,
    onPicked: (String?) -> Unit,
): () -> Unit {
    val types = remember(mimeTypes) { mimeTypes.toTypedArray() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        onPicked(uri?.toString())
    }

    return remember(launcher, types) {
        { launcher.launch(types) }
    }
}
