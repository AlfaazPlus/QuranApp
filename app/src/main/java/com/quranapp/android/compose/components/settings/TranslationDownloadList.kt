package com.quranapp.android.compose.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.viewModels.TranslationDownloadEvent
import java.util.regex.Pattern

@Composable
fun TranslationDownloadList(
    groups: List<TranslationGroupModel>,
    searchQuery: String,
    downloadStates: Map<String, ResourceDownloadStatus>,
    onEvent: (TranslationDownloadEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
) {
    val visibleGroups = remember(groups, searchQuery) {
        translationDownloadVisibleGroups(groups, searchQuery)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(visibleGroups, key = { it.langCode }) { group ->
            LanguageGroupCard(
                group = group,
                downloadStates = downloadStates,
                onEvent = onEvent,
            )
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

fun translationDownloadVisibleGroups(
    groups: List<TranslationGroupModel>,
    searchQuery: String,
): List<TranslationGroupModel> {
    if (searchQuery.isBlank()) {
        return groups
    }
    return groups.map { group ->
        val filteredTransls = group.translations.filter { transl ->
            val pattern = Pattern.compile(
                StringUtils.escapeRegex(searchQuery),
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

@Composable
private fun LanguageGroupCard(
    group: TranslationGroupModel,
    downloadStates: Map<String, ResourceDownloadStatus>,
    onEvent: (TranslationDownloadEvent) -> Unit,
) {
    val context = LocalContext.current
    val isAnyDownloading = downloadStates.values.any {
        it is ResourceDownloadStatus.Started || it is ResourceDownloadStatus.InProgress
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
                    .clickable(onClick = {
                        onEvent(TranslationDownloadEvent.ToggleGroup(group.langCode))
                    })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
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
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text =
                                stringResource(
                                    if (group.translations.size > 1)
                                        R.string.nItems else
                                        R.string.nItem, group.translations.size
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
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )

                    group.translations.forEachIndexed { index, model ->
                        val bookInfo = model.bookInfo
                        val slug = bookInfo.slug
                        val downloadState =
                            downloadStates[slug] ?: ResourceDownloadStatus.Idle

                        ItemRow(
                            model = model,
                            isAnyDownloading = isAnyDownloading,
                            downloadState = downloadState,
                            onDownload = {
                                if (!isAnyDownloading) {
                                    PeaceDialog.newBuilder(context)
                                        .setTitle(R.string.strTitleDownloadTranslations)
                                        .setMessage("${bookInfo.bookName}\n${bookInfo.authorName}")
                                        .setPositiveButton(R.string.labelDownload) { _, _ ->
                                            onEvent(TranslationDownloadEvent.DownloadTranslation(slug))
                                        }
                                        .setNegativeButton(R.string.strLabelCancel, null)
                                        .show()
                                }
                            },
                            onCancelDownload = {
                                PeaceDialog.newBuilder(context)
                                    .setTitle(R.string.titleCancelDownload)
                                    .setMessage("${bookInfo.bookName}\n${bookInfo.authorName}")
                                    .setPositiveButton(
                                        R.string.yesCancelDownload,
                                        ColorUtils.DANGER
                                    ) { _, _ ->
                                        onEvent(TranslationDownloadEvent.CancelDownload(slug))
                                    }
                                    .setNegativeButton(R.string.noContinueDownload, null)
                                    .show()
                            },
                        )

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
private fun ItemRow(
    model: TranslModel,
    isAnyDownloading: Boolean,
    downloadState: ResourceDownloadStatus,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    val appLocale = LocalAppLocale.current
    val bookInfo = model.bookInfo

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = !isAnyDownloading,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colorScheme.primary),
                    onClick = {
                        if (downloadState is ResourceDownloadStatus.Started || downloadState is ResourceDownloadStatus.InProgress) {
                            onCancelDownload()
                        } else {
                            onDownload()
                        }
                    }
                )
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = bookInfo.bookName,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                when (downloadState) {
                    is ResourceDownloadStatus.InProgress -> {
                        Text(
                            text = String.format(
                                appLocale.platformLocale,
                                "%1\$s %2\$d%%",
                                stringResource(R.string.textDownloading),
                                downloadState.progress
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            color = colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    is ResourceDownloadStatus.Started -> {
                        Text(
                            text = stringResource(R.string.textDownloading),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            color = colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    else -> {
                        Text(
                            text = bookInfo.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Light,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (downloadState) {
                is ResourceDownloadStatus.Started -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary
                    )
                }

                is ResourceDownloadStatus.InProgress -> {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceVariant
                        )

                        IconButton(
                            onClick = {
                                onCancelDownload()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_close),
                                contentDescription = stringResource(R.string.strLabelCancel),
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                is ResourceDownloadStatus.Failed -> {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_refresh),
                            contentDescription = stringResource(R.string.strLabelRetry),
                            tint = colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                else -> {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(40.dp),
                        enabled = !isAnyDownloading
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_download),
                            contentDescription = stringResource(R.string.labelDownload),
                            tint = if (isAnyDownloading) colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
