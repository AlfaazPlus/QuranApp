package com.quranapp.android.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.DocPage
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ErrorMessageCard
import com.quranapp.android.components.common.IconButton
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.components.importing.ContentImportSheet
import com.quranapp.android.components.importing.TranslationImportDetails
import com.quranapp.android.components.importing.translationImportFields
import com.quranapp.android.components.settings.TranslationSelectionMessages
import com.quranapp.android.models.transls.TranslModel
import com.quranapp.android.models.transls.TranslationGroupModel
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_chevron_right
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.dr_icon_refresh
import com.quranapp.android.generated.resources.ic_upload
import com.quranapp.android.generated.resources.item_count
import com.quranapp.android.generated.resources.msgDeleteTranslation
import com.quranapp.android.generated.resources.strHintSearch
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelDelete
import com.quranapp.android.generated.resources.strLabelUpdate
import com.quranapp.android.generated.resources.strTitleDownloadTranslations
import com.quranapp.android.generated.resources.strTitleTranslDelete
import com.quranapp.android.generated.resources.strTitleTranslations
import com.quranapp.android.generated.resources.updateAvailable
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.importing.ImportFieldId
import com.quranapp.android.utils.reader.translation.CustomTranslationImporter
import com.quranapp.android.utils.reader.translation.toTranslationManifest
import com.quranapp.android.utils.reader.translation.translationSlug
import com.quranapp.android.viewmodel.TranslationEvent
import com.quranapp.android.viewmodel.TranslationViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.util.regex.Pattern

@Composable
fun TranslationSelectionScreen() {
    val viewModel = appViewModel<TranslationViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val isDeveloperModeEnabled = AppPreferences.observeDeveloperModeEnabled()
    var importSheetOpen by rememberSaveable { mutableStateOf(false) }

    val visibleGroups = remember(uiState.translationGroups, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.translationGroups
        } else {
            uiState.translationGroups.map { group ->
                val filteredTransls = group.translations.filter { transl ->
                    val pattern = Pattern.compile(
                        Regex.escape(uiState.searchQuery),
                        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                    )

                    val bookInfo = transl.bookInfo
                    val bookName = bookInfo.bookName
                    val authorName = bookInfo.authorName
                    val langName = bookInfo.langName

                    pattern.matcher(bookName + authorName + langName).find()
                }

                group.copy(translations = ArrayList(filteredTransls))
            }.filter { it.translations.isNotEmpty() }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                stringResource(Res.string.strTitleTranslations),
                searchPlaceholder = stringResource(Res.string.strHintSearch),
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = {
                    viewModel.onEvent(TranslationEvent.Search(it))
                },
                actions = {
                    if (isDeveloperModeEnabled) {
                        SimpleTooltip(text = "Import") {
                            IconButton(
                                painter = painterResource(Res.drawable.ic_upload),
                                contentDescription = "Import",
                            ) {
                                importSheetOpen = true
                            }
                        }
                    }

                    IconButton(
                        painterResource(Res.drawable.dr_icon_refresh)
                    ) {
                        viewModel.onEvent(TranslationEvent.Refresh)
                    }
                }
            )
        },
        floatingActionButton = {
            DownloadTranslationsButton()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorMessageCard(
                    error = uiState.error!!,
                    onRetry = { viewModel.onEvent(TranslationEvent.Refresh) })

                else -> Content(
                    groups = visibleGroups,
                    selectedSlugs = uiState.selectedSlugs,
                    outdatedSlugs = uiState.outdatedSlugs,
                    updatingSlugs = uiState.updatingSlugs,
                )
            }
        }
    }

    val importFields = remember { translationImportFields() }
    val existingSlugs = remember(uiState.translationGroups) {
        uiState.translationGroups
            .flatMap { group -> group.translations.map { it.bookInfo.slug } }
            .toSet()
    }
    TranslationSelectionMessages(viewModel)

    ContentImportSheet(
        isOpen = importSheetOpen,
        onDismiss = { importSheetOpen = false },
        title = "Import",
        fields = importFields,
        doc = DocPage.CUSTOM_TRANSLATION,
        successMessage = "Translation added",
        onImported = { viewModel.onEvent(TranslationEvent.RefreshQuiet) },
        details = { TranslationImportDetails(it, existingSlugs) },
        canSubmit = { values ->
            translationSlug(
                values[ImportFieldId.LANG_CODE].orEmpty(),
                values[ImportFieldId.BOOK].orEmpty(),
            ) != null
        },
        buildManifest = { _, values -> values.toTranslationManifest() },
        onImport = { context, manifest, uri ->
            CustomTranslationImporter.import(context, manifest, uri)
        },
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp
        )
    }
}

@Composable
private fun Content(
    groups: List<TranslationGroupModel>,
    selectedSlugs: Set<String>,
    outdatedSlugs: Set<String>,
    updatingSlugs: Set<String>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(groups, key = { it.langCode }) { group ->
            LanguageGroupCard(
                group = group,
                selectedSlugs = selectedSlugs,
                outdatedSlugs = outdatedSlugs,
                updatingSlugs = updatingSlugs,
            )
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun LanguageGroupCard(
    group: TranslationGroupModel,
    selectedSlugs: Set<String>,
    outdatedSlugs: Set<String>,
    updatingSlugs: Set<String>,
) {
    val viewModel = appViewModel<TranslationViewModel>()

    val hasSelection = remember(group.translations, selectedSlugs) {
        group.translations.any { selectedSlugs.contains(it.bookInfo.slug) }
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 90f else 0f,
        animationSpec = tween(250),
        label = "chevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { viewModel.onEvent(TranslationEvent.ToggleGroup(group.langCode)) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.langName,
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (hasSelection) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = pluralStringResource(
                                Res.plurals.item_count,
                                group.translations.size,
                                group.translations.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp
                    )

                    group.translations.forEachIndexed { index, translation ->
                        TranslationRow(
                            translation = translation,
                            isSelected = selectedSlugs.contains(translation.bookInfo.slug),
                            needsUpdate = outdatedSlugs.contains(translation.bookInfo.slug),
                            isUpdating = updatingSlugs.contains(translation.bookInfo.slug),
                            onCheckChanged = {
                                viewModel.onEvent(
                                    TranslationEvent.SelectionChanged(
                                        translation, it
                                    )
                                )
                            },
                            onDelete = {
                                viewModel.onEvent(
                                    TranslationEvent.DeleteTranslation(
                                        translation.bookInfo.slug
                                    )
                                )
                            },
                            onUpdate = {
                                viewModel.onEvent(
                                    TranslationEvent.UpdateTranslation(
                                        translation.bookInfo.slug,
                                    ),
                                )
                            })

                        if (index < group.translations.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationRow(
    translation: TranslModel,
    isSelected: Boolean,
    needsUpdate: Boolean,
    isUpdating: Boolean,
    onCheckChanged: (isChecked: Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val bookInfo = translation.bookInfo

    if (confirmDelete) {
        AlertDialog(
            isOpen = true,
            onClose = { confirmDelete = false },
            title = stringResource(Res.string.strTitleTranslDelete),
            actions = listOf(
                AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelDelete),
                    style = AlertDialogActionStyle.Danger,
                    onClick = onDelete,
                ),
            ),
        ) {
            Text(
                text = stringResource(
                    Res.string.msgDeleteTranslation,
                    bookInfo.bookName,
                    bookInfo.authorName,
                ),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colorScheme.primary),
                    onClick = { onCheckChanged(!isSelected) })
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(0.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bookInfo.bookName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = bookInfo.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Light,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (needsUpdate) {
                    Text(
                        text = stringResource(Res.string.updateAvailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }

        if (!TranslUtils.isPrebuilt(bookInfo.slug)) {
            VerticalDivider(
                modifier = Modifier
                    .height(38.dp)
                    .width(1.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            Box(
                modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center
            ) {
                if (needsUpdate) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(
                            onClick = onUpdate,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_refresh),
                                contentDescription = stringResource(Res.string.strLabelUpdate),
                                tint = colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_delete),
                            contentDescription = stringResource(Res.string.strLabelDelete),
                            tint = colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTranslationsButton() {
    val navController = LocalSettingsNavController.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                navController.navigate(
                    route = SettingRoutes.TRANSLATIONS_DOWNLOAD
                )
            }
            .background(colorScheme.primary)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                4.dp,
                Alignment.CenterHorizontally
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_download),
                contentDescription = stringResource(Res.string.strTitleDownloadTranslations),
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = stringResource(Res.string.strTitleDownloadTranslations),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimary
            )
        }
    }
}
