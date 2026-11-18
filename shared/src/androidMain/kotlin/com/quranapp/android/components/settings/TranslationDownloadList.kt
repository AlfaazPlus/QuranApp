package com.quranapp.android.components.settings

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.models.transls.TranslModel
import com.quranapp.android.models.transls.TranslationGroupModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_chevron_right
import com.quranapp.android.generated.resources.dr_icon_close
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.dr_icon_refresh
import com.quranapp.android.generated.resources.item_count
import com.quranapp.android.generated.resources.labelDownload
import com.quranapp.android.generated.resources.noContinueDownload
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelRetry
import com.quranapp.android.generated.resources.strTitleDownloadTranslations
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.generated.resources.titleCancelDownload
import com.quranapp.android.generated.resources.yesCancelDownload
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.viewmodel.TranslationDownloadEvent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.util.regex.Pattern

@Composable
fun TranslationDownloadList(
    groups: List<TranslationGroupModel>,
    searchQuery: String,
    downloadStates: Map<String, ResourceDownloadStatus>,
    onEvent: (TranslationDownloadEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 16.dp,
        bottom = 48.dp
    ),
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
                Regex.escape(searchQuery),
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
    var confirm by remember { mutableStateOf<TranslationDownloadConfirm?>(null) }
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
                                    confirm = TranslationDownloadConfirm.Download(
                                        slug = slug,
                                        message = "${bookInfo.bookName}\n${bookInfo.authorName}",
                                    )
                                }
                            },
                            onCancelDownload = {
                                confirm = TranslationDownloadConfirm.Cancel(
                                    slug = slug,
                                    message = "${bookInfo.bookName}\n${bookInfo.authorName}",
                                )
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

    val pending = confirm
    if (pending != null) {
        val title = when (pending) {
            is TranslationDownloadConfirm.Download ->
                stringResource(Res.string.strTitleDownloadTranslations)

            is TranslationDownloadConfirm.Cancel ->
                stringResource(Res.string.titleCancelDownload)
        }
        val positive = when (pending) {
            is TranslationDownloadConfirm.Download ->
                stringResource(Res.string.labelDownload)

            is TranslationDownloadConfirm.Cancel ->
                stringResource(Res.string.yesCancelDownload)
        }
        val negative = when (pending) {
            is TranslationDownloadConfirm.Download ->
                stringResource(Res.string.strLabelCancel)

            is TranslationDownloadConfirm.Cancel ->
                stringResource(Res.string.noContinueDownload)
        }

        AlertDialog(
            isOpen = true,
            onClose = { confirm = null },
            title = title,
            actions = listOf(
                AlertDialogAction(
                    text = negative,
                    onClick = { confirm = null },
                ),
                AlertDialogAction(
                    text = positive,
                    style = if (pending is TranslationDownloadConfirm.Cancel) {
                        AlertDialogActionStyle.Danger
                    } else {
                        AlertDialogActionStyle.Primary
                    },
                    onClick = {
                        when (pending) {
                            is TranslationDownloadConfirm.Download ->
                                onEvent(TranslationDownloadEvent.DownloadTranslation(pending.slug))

                            is TranslationDownloadConfirm.Cancel ->
                                onEvent(TranslationDownloadEvent.CancelDownload(pending.slug))
                        }
                        confirm = null
                    },
                ),
            ),
        ) {
            Text(text = pending.message)
        }
    }
}

private sealed interface TranslationDownloadConfirm {
    val slug: String
    val message: String

    data class Download(
        override val slug: String,
        override val message: String,
    ) : TranslationDownloadConfirm

    data class Cancel(
        override val slug: String,
        override val message: String,
    ) : TranslationDownloadConfirm
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
                            text = appLocale.format(
                                "%1\$s %2\$d%%",
                                stringResource(Res.string.textDownloading),
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
                            text = stringResource(Res.string.textDownloading),
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
                                painter = painterResource(Res.drawable.dr_icon_close),
                                contentDescription = stringResource(Res.string.strLabelCancel),
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
                            painter = painterResource(Res.drawable.dr_icon_refresh),
                            contentDescription = stringResource(Res.string.strLabelRetry),
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
                            painter = painterResource(Res.drawable.dr_icon_download),
                            contentDescription = stringResource(Res.string.labelDownload),
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
