package com.quranapp.android.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.extensions.verticalFadingEdge
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.donate
import com.quranapp.android.generated.resources.dr_icon_close
import com.quranapp.android.generated.resources.dr_icon_hamburger
import com.quranapp.android.generated.resources.dr_icon_info
import com.quranapp.android.generated.resources.dr_icon_rate
import com.quranapp.android.generated.resources.dr_icon_settings
import com.quranapp.android.generated.resources.dr_icon_share
import com.quranapp.android.generated.resources.ic_donate
import com.quranapp.android.generated.resources.icon_clean
import com.quranapp.android.generated.resources.icon_import_export
import com.quranapp.android.generated.resources.strDescClose
import com.quranapp.android.generated.resources.strMsgShareApp
import com.quranapp.android.generated.resources.strTitleAboutUs
import com.quranapp.android.generated.resources.strTitleDownloadSunnahApp
import com.quranapp.android.generated.resources.strTitleMenu
import com.quranapp.android.generated.resources.strTitleRateApp
import com.quranapp.android.generated.resources.strTitleSettings
import com.quranapp.android.generated.resources.strTitleShareApp
import com.quranapp.android.generated.resources.sunnahapp_logo_icon
import com.quranapp.android.generated.resources.titleExportData
import com.quranapp.android.generated.resources.titleStorageCleanup
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.app.UrlsManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private data class IndexMenuItemGroup(
    val items: List<IndexMenuItem>,
)

private data class IndexMenuItem(
    val icon: DrawableResource,
    val title: StringResource,

    val iconTint: Color? = null,
    val textColor: Color? = null,
    val onClick: () -> Unit,
)

@Composable
private fun getItems(onDonate: () -> Unit): List<IndexMenuItemGroup> {
    val scope = rememberCoroutineScope()

    return listOf(
        IndexMenuItemGroup(
            listOf(
                IndexMenuItem(
                    Res.drawable.dr_icon_settings,
                    Res.string.strTitleSettings,
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Screen(AppRoutes.SETTINGS))
                    }
                ),
                IndexMenuItem(
                    Res.drawable.icon_clean,
                    Res.string.titleStorageCleanup,
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Screen(AppRoutes.STORAGE_CLEANUP))
                    }
                ),
                IndexMenuItem(
                    Res.drawable.icon_import_export,
                    Res.string.titleExportData,
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Screen(AppRoutes.EXPORT_IMPORT))
                    }
                ),
            )
        ),
        IndexMenuItemGroup(
            listOf(
                IndexMenuItem(
                    Res.drawable.dr_icon_info,
                    Res.string.strTitleAboutUs,
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Screen(AppRoutes.ABOUT))
                    }
                ),
                IndexMenuItem(
                    Res.drawable.dr_icon_rate,
                    Res.string.strTitleRateApp,
                    onClick = {
                        Platform.links.openStore()
                    }
                ),
                IndexMenuItem(
                    Res.drawable.dr_icon_share,
                    Res.string.strTitleShareApp,
                    onClick = {
                        scope.launch {
                            Platform.links.shareText(
                                text = getString(
                                    Res.string.strMsgShareApp,
                                    Platform.links.storeWebLink(),
                                ),
                                chooserTitle = getString(Res.string.strTitleShareApp),
                            )
                        }
                    }
                ),
                IndexMenuItem(
                    Res.drawable.sunnahapp_logo_icon,
                    Res.string.strTitleDownloadSunnahApp,
                    iconTint = colorScheme.primary,
                    onClick = {
                        Platform.links.openStore("com.alfaazplus.sunnah")
                    }
                ),
                IndexMenuItem(
                    Res.drawable.ic_donate,
                    Res.string.donate,
                    iconTint = colorScheme.primary,
                    onClick = { onDonate() }
                ),
            )
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexMenuButton() {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    val openLink = rememberInfoLinks()

    val config = LocalConfiguration.current
    val sheetMaxWidth = config.screenWidthDp.dp * .95f
    val sheetWidthDiff = (config.screenWidthDp.dp - sheetMaxWidth) / 2

    SimpleTooltip(text = stringResource(Res.string.strTitleMenu)) {
        IconButton(
            modifier = Modifier.size(40.dp),
            onClick = {
                showMenu = true
            }
        ) {
            Icon(
                painter = painterResource(
                    Res.drawable.dr_icon_hamburger
                ),
                contentDescription = stringResource(Res.string.strTitleMenu),
                tint = colorScheme.onSurface
            )
        }
    }

    if (!showMenu) return

    ModalBottomSheet(
        modifier = Modifier
            .padding(
                bottom = sheetWidthDiff
            ),
        onDismissRequest = {
            showMenu = false
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = colorScheme.onSurface,
        shape = shapes.large,
        scrimColor = Color.Black.alpha(0.5f),
        dragHandle = null,
        sheetMaxWidth = minOf(sheetMaxWidth, BottomSheetDefaults.SheetMaxWidth),
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
        }
    ) {
        IndexMenuContent(onDonate = { openLink(UrlsManager.URL_KEY_DONATION) }) {
            showMenu = false
        }
    }
}

@Composable
fun IndexMenuContent(
    onDonate: () -> Unit,
    onClose: () -> Unit,
) {
    val config = LocalConfiguration.current

    val maxMenuHeight = config.screenHeightDp.dp * 0.8f
    val scrollState = rememberScrollState()
    val items = getItems(onDonate)

    Column(
        modifier = Modifier
            .heightIn(max = maxMenuHeight)
            .background(colorScheme.surface, RoundedCornerShape(20.dp))
            .border(
                1.dp,
                colorScheme.outlineVariant.alpha(0.5f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_hamburger),
                contentDescription = null,
                modifier = Modifier.size(25.dp)
            )

            Text(
                text = stringResource(Res.string.strTitleMenu),
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .weight(1f),
                style = typography.titleLarge,
            )

            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                onClick = onClose
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_close),
                    contentDescription = stringResource(Res.string.strDescClose),
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = colorScheme.outlineVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalFadingEdge(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(top = 12.dp, bottom = 20.dp)
            ) {
                items.forEachIndexed { groupIndex, group ->
                    group.items.forEachIndexed { _, item ->
                        IndexMenuItemRow(item, onClose)
                    }

                    if (groupIndex < items.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexMenuItemRow(
    item: IndexMenuItem,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                item.onClick()
                onClose()
            })
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = item.iconTint ?: LocalContentColor.current
        )

        Spacer(modifier = Modifier.width(17.dp))

        Text(
            text = stringResource(item.title),
            color = item.textColor ?: colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
