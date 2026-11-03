package com.quranapp.android.compose.screens.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.ExportKeys
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.ListItem
import kotlinx.coroutines.launch

internal data class ExportOption(
    val key: String,
    val title: Int,
    val description: Int,
    val icon: Int,
)

internal fun dataOptions(forExport: Boolean) = listOf(
    ExportOption(
        ExportKeys.SETTINGS,
        R.string.strTitleSettings,
        if (forExport) R.string.msgExportSettings else R.string.msgImportSettings,
        R.drawable.dr_icon_settings,
    ),
    ExportOption(
        ExportKeys.BOOKMARKS,
        R.string.strTitleBookmarks,
        if (forExport) R.string.msgExportBookmarks else R.string.msgImportBookmarks,
        R.drawable.ic_bookmark,
    ),
    ExportOption(
        ExportKeys.COLLECTIONS,
        R.string.labelCollections,
        if (forExport) R.string.msgExportCollections else R.string.msgImportCollections,
        R.drawable.ic_library,
    ),
)

internal val exportSelectionSaver = listSaver<Set<String>, String>(
    save = { value -> value.toList() },
    restore = { saved -> saved.toSet() },
)

internal fun selectedScopes(selectedItems: Set<String>): Map<String, Boolean> {
    return dataOptions(forExport = true).associate { it.key to (it.key in selectedItems) }
}

@Composable
fun ExportImportScreen(
    importFileName: String?,
    onPickImportFile: () -> Unit,
    onImport: (scopes: Map<String, Boolean>) -> Unit,
    exportCallback: (scopes: Map<String, Boolean>) -> Unit,
) {
    val tabs = listOf(R.string.labelExport, R.string.labelImport)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(R.string.titleExportData),
                shadowElevation = 0.dp,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(stringResource(title), style = typography.labelLarge) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) { page ->
                if (page == 0) ExportPage(onExport = exportCallback)
                if (page == 1) {
                    ImportPage(
                        fileName = importFileName,
                        onPickFile = onPickImportFile,
                        onImport = onImport,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CategorySelection(
    forExport: Boolean,
    selectedItems: Set<String>,
    onSelectedItemsChange: (Set<String>) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val options = dataOptions(forExport)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.medium,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {
        Column() {
            options.forEachIndexed { index, option ->
                DataOptionRow(
                    option = option,
                    checked = option.key in selectedItems,
                    onCheckedChange = { checked ->
                        onSelectedItemsChange(
                            if (checked) selectedItems + option.key else selectedItems - option.key
                        )
                    },
                )

                if (index < options.lastIndex) {
                    HorizontalDivider(color = colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
internal fun SelectedActionButton(
    text: String,
    icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(50),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = typography.titleMedium,
        )
    }
}

@Composable
private fun DataOptionRow(
    option: ExportOption,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        title = option.title,
        subtitle = option.description,
        leading = {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    ) { onCheckedChange(!checked) }
}
