package com.quranapp.android.compose.screens.settings

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.peacedesign.android.utils.ColorUtils
import com.quranapp.android.R
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.navigation.LocalSettingsNavHostController
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.viewModels.TranslationEvent
import com.quranapp.android.viewModels.TranslationViewModel
import java.util.regex.Pattern

@Composable
fun TranslationSelectionScreen() {
    val viewModel = viewModel<TranslationViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val visibleGroups = remember(uiState.translationGroups, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.translationGroups
        } else {
            uiState.translationGroups.map { group ->
                val filteredTransls = group.translations.filter { transl ->
                    val pattern = Pattern.compile(
                        StringUtils.escapeRegex(uiState.searchQuery),
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
                stringResource(R.string.strTitleTranslations),
                actions = {
                    // todo: search
                    IconButton(
                        painterResource(R.drawable.dr_icon_refresh)
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
                )
            }
        }
    }
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
            )
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun LanguageGroupCard(
    group: TranslationGroupModel,
    selectedSlugs: Set<String>,
) {
    val viewModel = viewModel<TranslationViewModel>()

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
                                style = MaterialTheme.typography.titleSmall,
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
                            text = stringResource(
                                if (group.translations.size > 1) R.string.nItems else R.string.nItem,
                                group.translations.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_right),
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
    onCheckChanged: (isChecked: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val bookInfo = translation.bookInfo

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
                IconButton(
                    onClick = {
                        MessageUtils.showConfirmationDialog(
                            context = context,
                            title = context.getString(R.string.strTitleTranslDelete),
                            msg = context.getString(
                                R.string.msgDeleteTranslation,
                                bookInfo.bookName,
                                bookInfo.authorName
                            ),
                            btn = context.getString(R.string.strLabelDelete),
                            btnColor = ColorUtils.DANGER,
                            action = {
                                onDelete()
                            })
                    }, modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_delete),
                        contentDescription = stringResource(R.string.strLabelDelete),
                        tint = colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTranslationsButton() {
    val navController = LocalSettingsNavHostController.current

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
                painter = painterResource(R.drawable.dr_icon_download),
                contentDescription = stringResource(R.string.strTitleDownloadTranslations),
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = stringResource(R.string.strTitleDownloadTranslations),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimary
            )
        }
    }
}