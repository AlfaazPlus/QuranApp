package com.quranapp.android.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.appLogs.AppLogModel
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.IconButton
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.appLogs
import com.quranapp.android.generated.resources.copiedToClipboard
import com.quranapp.android.generated.resources.crashLogs
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.github
import com.quranapp.android.generated.resources.icon_copy
import com.quranapp.android.generated.resources.icon_github
import com.quranapp.android.generated.resources.logRemoved
import com.quranapp.android.generated.resources.pasteCrashLogGithubIssue
import com.quranapp.android.generated.resources.strLabelCopy
import com.quranapp.android.generated.resources.strLabelDelete
import com.quranapp.android.generated.resources.suppressedLogs
import com.quranapp.android.generated.resources.textNoLogsFound
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLogsScreen() {
    val tabs = listOf(Res.string.crashLogs, Res.string.suppressedLogs)

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { AppBar(title = stringResource(Res.string.appLogs), shadowElevation = 0.dp) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                    .fillMaxHeight()
            ) { page ->
                if (page == 0) CrashLogs()
                if (page == 1) SuppressedLogs()
            }
        }
    }
}

@Composable
private fun CrashLogs() {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<AppLogModel>>(emptyList()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val files = Logger.CRASH_LOGS_DIR.toFile().listFiles()

        if (files.isNullOrEmpty()) {
            logs = emptyList()
        } else {
            logs = files
                .sortedByDescending { it.lastModified() }
                .map { logFile ->
                    val logText = logFile.readText()
                    val logShort = if (logText.length > 200) logText.substring(
                        0,
                        200
                    ) + "... ${logText.length - 200} more chars"
                    else logText

                    val parsedDate = DateUtils.parse(logFile.name, DateUtils.FILENAME)
                    val formattedDateTime =
                        parsedDate?.let { DateUtils.format(it) } ?: logFile.name

                    AppLogModel(
                        datetime = formattedDateTime,
                        place = "Fatal Crash",
                        file = logFile,
                        log = logText,
                        logShort = logShort,
                    )
                }
        }

        isLoading = false
    }

    if (isLoading) {
        return Loader(true)
    }

    if (logs.isEmpty()) {
        return Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.textNoLogsFound),
                textAlign = TextAlign.Center,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(logs.size) { index ->
            val log = logs[index]
            AppLogItem(
                log,
                isCrash = true,
            ) {
                it.file.delete()
                logs = logs
                    .toMutableList()
                    .apply { remove(it) }

                scope.launch {
                    PlatformServices.messages.show(getString(Res.string.logRemoved))
                }
            }
        }
    }
}

@Composable
private fun SuppressedLogs() {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<AppLogModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        val files = Logger.SUPPRESSED_LOGS_DIR.toFile().listFiles()

        if (files.isNullOrEmpty()) {
            logs = emptyList()
        } else {
            logs = files
                .sortedByDescending { it.lastModified() }
                .map { logFile ->
                    val (datetimeStr, location) = logFile.nameWithoutExtension.split("@")

                    val logText = logFile.readText()
                    val logShort = if (logText.length > 200) logText.substring(
                        0,
                        200
                    ) + "... ${logText.length - 200} more chars"
                    else logText

                    val parsedDate = DateUtils.parse(datetimeStr, DateUtils.FILENAME)
                    val formattedDateTime =
                        parsedDate?.let { DateUtils.format(it) } ?: logFile.name

                    AppLogModel(
                        datetime = formattedDateTime,
                        place = location,
                        file = logFile,
                        log = logText,
                        logShort = logShort,
                    )
                }
        }

        isLoading = false
    }

    if (isLoading) {
        return Loader(true)
    }

    if (logs.isEmpty()) {
        return Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.textNoLogsFound),
                textAlign = TextAlign.Center,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(logs.size) { index ->
            val log = logs[index]
            AppLogItem(
                log,
                isCrash = false,
            ) {
                it.file.delete()
                logs = logs
                    .toMutableList()
                    .apply { remove(it) }

                scope.launch {
                    PlatformServices.messages.show(getString(Res.string.logRemoved))
                }
            }
        }
    }
}

@Composable
private fun AppLogItem(log: AppLogModel, isCrash: Boolean, handleDelete: (AppLogModel) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = shapes.medium,
                spotColor = Color.Black.alpha(0.3f),
            )
            .background(MaterialTheme.colorScheme.surface, shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.alpha(0.5f), shapes.medium)
    ) {
        Column {
            Text(
                text = log.place,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.alpha(0.5f),
                thickness = 1.dp,
            )

            Text(
                text = log.logShort,
                color = if (isCrash) Color.Red else Color(0xFFB69200),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )

            HorizontalDivider(
                color = colorScheme.outlineVariant.alpha(0.5f),
                thickness = 1.dp,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = log.datetime,
                    modifier = Modifier.weight(1f),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurface,
                )
                SimpleTooltip(text = stringResource(Res.string.strLabelDelete)) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        onClick = {
                            handleDelete(log)
                        },
                        tint = colorScheme.onSurface,
                        small = true,
                    )
                }
                SimpleTooltip(text = stringResource(Res.string.strLabelCopy)) {
                    IconButton(
                        painter = painterResource(Res.drawable.icon_copy),
                        onClick = {
                            PlatformServices.system.clipboardManager.copy(log.log)
                            scope.launch {
                                PlatformServices.messages.showClipboard(
                                    getString(Res.string.copiedToClipboard),
                                )
                            }
                        },
                        tint = colorScheme.onSurface,
                        small = true,
                    )
                }

                SimpleTooltip(text = stringResource(Res.string.github)) {
                    Button(
                        onClick = {
                            PlatformServices.system.clipboardManager.copy(log.log)
                            scope.launch {
                                PlatformServices.messages.show(
                                    getString(Res.string.pasteCrashLogGithubIssue),
                                    long = true
                                )
                            }
                            PlatformServices.links.browse(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL)
                        },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(100),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.icon_github),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

