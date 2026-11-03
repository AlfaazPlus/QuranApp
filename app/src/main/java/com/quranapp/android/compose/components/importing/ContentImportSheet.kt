package com.quranapp.android.compose.components.importing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.R
import com.quranapp.android.api.DocPage
import com.quranapp.android.compose.components.common.SearchTextField
import com.quranapp.android.compose.components.common.TextInput
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import com.quranapp.android.utils.reader.importing.ImportFieldId
import com.quranapp.android.utils.reader.importing.ImportManifest
import com.quranapp.android.utils.reader.importing.readImportFileName
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportFieldSpan { Full, Half }

data class ImportChoice(
    val value: String,
    val label: String,
)

data class ImportField(
    val id: ImportFieldId,
    val label: Int,
    val required: Boolean,
    val placeholder: String? = null,
    val initial: String = "",
    val span: ImportFieldSpan = ImportFieldSpan.Full,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val transform: (String) -> String = { it },
    val choices: List<ImportChoice>? = null,
)

private val formSaver = listSaver<Map<ImportFieldId, String>, String>(
    save = { value -> value.flatMap { listOf(it.key.name, it.value) } },
    restore = { saved ->
        saved.chunked(2).associate { ImportFieldId.valueOf(it[0]) to it[1] }
    },
)

private val uriSaver = listSaver<Uri?, String>(
    save = { listOf(it?.toString().orEmpty()) },
    restore = { saved -> saved.first().takeIf { it.isNotEmpty() }?.let(Uri::parse) },
)

@Composable
fun ContentImportSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    title: String,
    fields: List<ImportField>,
    doc: DocPage,
    successMessage: String,
    onImported: () -> Unit,
    details: @Composable (Map<ImportFieldId, String>) -> Unit = {},
    canSubmit: (Map<ImportFieldId, String>) -> Boolean = { true },
    buildManifest: (Context, Map<ImportFieldId, String>) -> ImportManifest,
    onImport: suspend (Context, ImportManifest, Uri) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialValues = remember(fields) { fields.associate { it.id to it.initial } }
    var values by rememberSaveable(stateSaver = formSaver) { mutableStateOf(initialValues) }
    var fileUri by rememberSaveable(stateSaver = uriSaver) { mutableStateOf(null) }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            values = initialValues
            fileUri = null
            fileName = null
            importError = null
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        fileUri = uri
        fileName = readImportFileName(context, uri)
        importError = null
    }

    val requiredFilled = fields.filter { it.required }.all { field ->
        values[field.id].orEmpty().isNotBlank()
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.ic_upload,
        title = title,
        headerArrangement = Arrangement.Start,
        sheetGesturesEnabled = false,
        dismissible = false,
        actions = {
            TextButton(
                onClick = { AppBridge.newOpener(context).browseLink(doc.url()) },
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_help),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(text = stringResource(R.string.labelFormatGuide))
            }
        },
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).union(WindowInsets.ime)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            fieldRows(fields).forEach { row ->
                if (row.size == 1) {
                    ImportFieldInput(row.first(), values, importing) { id, updated ->
                        values = values + (id to updated)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { field ->
                            ImportFieldInput(
                                field = field,
                                values = values,
                                importing = importing,
                                modifier = Modifier.weight(1f),
                            ) { id, updated ->
                                values = values + (id to updated)
                            }
                        }
                    }
                }
            }

            details(values)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceContainerLow)
                    .clickable(enabled = !importing) {
                        fileLauncher.launch(
                            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(
                                    Intent.EXTRA_MIME_TYPES,
                                    arrayOf("application/json", "text/plain"),
                                )
                            },
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.col_folder_open),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )

                Text(
                    text = fileName ?: stringResource(R.string.labelSelectFile),
                    modifier = Modifier.weight(1f),
                    style = typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            importError?.let { message ->
                Text(
                    text = message,
                    color = colorScheme.error,
                    style = typography.bodyMedium,
                )
            }
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
                onClick = onDismiss,
                enabled = !importing,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(text = stringResource(R.string.strLabelCancel))
            }

            Button(
                onClick = {
                    val uri = fileUri ?: return@Button
                    scope.launch {
                        importing = true
                        importError = null

                        val failure = withContext(Dispatchers.IO) {
                            runCatching {
                                val manifest = buildManifest(context, values)
                                onImport(context, manifest, uri)
                            }.exceptionOrNull()
                        }

                        importing = false

                        if (failure == null) {
                            MessageUtils.showRemovableToast(
                                context,
                                successMessage,
                                Toast.LENGTH_LONG,
                            )

                            onImported()
                            onDismiss()
                        } else {
                            val explicit = (failure as? ImportException)?.message
                            if (!explicit.isNullOrBlank()) {
                                importError = explicit
                            } else {
                                val messageRes = when ((failure as? ImportException)?.failure) {
                                    ImportFailure.DETAILS -> R.string.importDetailsInvalid
                                    ImportFailure.RESERVED -> R.string.importReserved
                                    else -> R.string.importInvalid
                                }

                                MessageUtils.showRemovableToast(
                                    context,
                                    messageRes,
                                    Toast.LENGTH_LONG
                                )
                            }
                        }
                    }
                },
                enabled = requiredFilled && fileUri != null && canSubmit(values) && !importing,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (importing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onPrimary,
                    )
                } else {
                    Text(text = stringResource(R.string.labelImport))
                }
            }
        }
    }
}

private fun fieldRows(fields: List<ImportField>): List<List<ImportField>> {
    val rows = mutableListOf<List<ImportField>>()
    var index = 0

    while (index < fields.size) {
        val field = fields[index]
        val next = fields.getOrNull(index + 1)

        if (field.span == ImportFieldSpan.Half && next?.span == ImportFieldSpan.Half) {
            rows += listOf(field, next)
            index += 2
        } else {
            rows += listOf(field)
            index += 1
        }
    }

    return rows
}

@Composable
private fun ImportFieldInput(
    field: ImportField,
    values: Map<ImportFieldId, String>,
    importing: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (ImportFieldId, String) -> Unit,
) {
    val choices = field.choices
    if (choices != null) {
        ImportChoiceField(
            label = stringResource(field.label),
            value = values[field.id].orEmpty(),
            choices = choices,
            enabled = !importing,
            modifier = modifier,
            onValueChange = { onValueChange(field.id, it) },
        )
        return
    }

    TextInput(
        modifier = modifier,
        value = values[field.id].orEmpty(),
        onValueChange = { updated -> onValueChange(field.id, field.transform(updated)) },
        label = stringResource(field.label),
        placeholder = field.placeholder,
        maxLines = 1,
        enabled = !importing,
        keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
    )
}

@Composable
private fun ImportChoiceField(
    label: String,
    value: String,
    choices: List<ImportChoice>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.value == value }

    if (pickerOpen) {
        ImportChoicePicker(
            label = label,
            value = value,
            choices = choices,
            onDismiss = { pickerOpen = false },
            onSelect = { selectedValue ->
                onValueChange(selectedValue)
                pickerOpen = false
            },
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = colorScheme.background,
                focusedContainerColor = colorScheme.background,
                disabledContainerColor = colorScheme.background,
                unfocusedLabelColor = colorScheme.onBackground.alpha(0.6f),
                unfocusedBorderColor = colorScheme.outline.alpha(0.3f),
                focusedBorderColor = colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { pickerOpen = true },
        )
    }
}

@Composable
private fun ImportChoicePicker(
    label: String,
    value: String,
    choices: List<ImportChoice>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleChoices = remember(query, choices) {
        if (query.isBlank()) {
            choices
        } else {
            choices.filter { choice ->
                choice.label.contains(query, ignoreCase = true) ||
                        choice.value.contains(query, ignoreCase = true)
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val index = choices.indexOfFirst { it.value == value }
        if (index > 0) listState.scrollToItem((index - 1).coerceAtLeast(0))
    }

    BottomSheet(
        isOpen = true,
        onDismiss = onDismiss,
        title = label,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).union(WindowInsets.ime)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            SearchTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                singleLine = true,
                placeholder = stringResource(R.string.strHintSearch),
            )

            if (visibleChoices.isEmpty()) {
                Text(
                    text = stringResource(R.string.noResults),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    style = typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(visibleChoices, key = { it.value }) { choice ->
                        val selected = choice.value == value

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onSelect(choice.value) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = choice.label,
                                modifier = Modifier.weight(1f),
                                style = typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) colorScheme.primary else colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Box(
                                modifier = Modifier.size(18.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        painter = painterResource(R.drawable.dr_icon_check),
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            Text(
                                text = choice.value,
                                style = typography.labelLarge,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
