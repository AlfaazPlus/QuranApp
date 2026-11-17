package com.quranapp.android.screens.shareimage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ErrorMessageCard
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.settings.SettingsItem
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.content
import com.quranapp.android.generated.resources.customize
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.dr_icon_quran_script
import com.quranapp.android.generated.resources.dr_icon_share
import com.quranapp.android.generated.resources.dr_icon_translations
import com.quranapp.android.generated.resources.ic_image
import com.quranapp.android.generated.resources.labelDownload
import com.quranapp.android.generated.resources.labelTranslation
import com.quranapp.android.generated.resources.shareAsImage
import com.quranapp.android.generated.resources.strLabelShare
import com.quranapp.android.generated.resources.strTitleScripts
import com.quranapp.android.generated.resources.templates
import com.quranapp.android.models.DataLoadError
import com.quranapp.android.utils.reader.getQuranScriptName
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShareAsImageScreen(
    chapterNo: Int,
    verseNo: Int,
) {
    val showTwoPane = currentWindowAdaptiveInfoV2().windowSizeClass.isAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )

    ShareAsImageProvider(
        chapterNo = chapterNo,
        verseNo = verseNo,
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(Res.string.shareAsImage),
                    shadowElevation = 0.dp,
                )
            },
            containerColor = colorScheme.background,
        ) { padding ->
            val session = LocalShareAsImage.current

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val shortAndWide = maxWidth >= 600.dp && maxHeight < 480.dp
                val childModifier = Modifier.fillMaxSize()

                when {
                    session.templates.isEmpty() && session.isTemplatesLoading -> {
                        Box(
                            modifier = childModifier,
                            contentAlignment = Alignment.Center,
                        ) {
                            Loader()
                        }
                    }

                    session.templates.isEmpty() -> {
                        ErrorMessageCard(
                            error = session.templatesError ?: DataLoadError.Failed,
                            onRetry = session.onRetryTemplates,
                            modifier = childModifier,
                        )
                    }

                    showTwoPane || shortAndWide -> TabletShareLayout(modifier = childModifier)
                    else -> PhoneShareLayout(modifier = childModifier)
                }
            }
        }
    }
}

@Composable
fun ShareOptionsPane(
    columnCount: Int,
    modifier: Modifier = Modifier,
) {
    val session = LocalShareAsImage.current
    val tabs = listOf(Res.string.templates, Res.string.customize)

    Column(modifier = modifier) {
        PrimaryTabRow(
            selectedTabIndex = session.selectedTabIndex,
            containerColor = colorScheme.surfaceContainer,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(stringResource(title), style = typography.labelLarge) },
                    selected = session.selectedTabIndex == index,
                    onClick = { session.onSelectTab(index) },
                    selectedContentColor = colorScheme.primary,
                    unselectedContentColor = colorScheme.onSurface,
                )
            }
        }

        if (session.selectedTabIndex == 0) {
            TemplatesGrid(
                templates = session.templates,
                columnCount = columnCount,
                selectedId = session.selectedTemplate?.id,
                onSelect = session.onSelectTemplate,
                state = session.templatesGridState,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ShareCustomizeControls(
                session = session,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun ShareCustomizeControls(
    session: ShareAsImageSession,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(session.customizeScrollState)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsItem(
            icon = Res.drawable.ic_image,
            title = Res.string.content,
            subtitle = session.contentOption.labelRes,
            onClick = session.onOpenContent,
        )

        if (session.contentOption.showTranslation) {
            SettingsItem(
                icon = Res.drawable.dr_icon_translations,
                title = Res.string.labelTranslation,
                subtitleStr = session.selectedBook.shareLabel()
                    ?: stringResource(Res.string.labelTranslation),
                onClick = session.onOpenTranslation,
            )
        }

        if (session.contentOption.showArabic) {
            SettingsItem(
                icon = Res.drawable.dr_icon_quran_script,
                title = Res.string.strTitleScripts,
                subtitleStr = session.scriptCode.getQuranScriptName(),
                onClick = session.onOpenScript,
            )
        }
    }
}

private fun TranslationBookInfoModel?.shareLabel(): String? {
    val book = this ?: return null
    return book.displayName.ifBlank { book.bookName }.ifBlank { null }
}

@Composable
fun ShareDownloadBar(
    canExport: Boolean,
    isExporting: Boolean,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    val enabled = canExport && !isExporting

    Row(
        modifier = modifier
            .shadow(4.dp)
            .background(colorScheme.surfaceContainer)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onShare,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
            ),
            shape = shapes.large,
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_share),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.strLabelShare))
        }

        IconButton(
            onClick = onDownload,
            enabled = enabled,
            modifier = Modifier
                .size(52.dp)
                .border(1.dp, colorScheme.outlineVariant, shapes.large),
            shape = shapes.large,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_download),
                contentDescription = stringResource(Res.string.labelDownload),
            )
        }
    }
}
