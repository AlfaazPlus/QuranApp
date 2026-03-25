package com.quranapp.android.compose.screens.settings

import android.widget.Toast
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.appLogs.AppLogModel
import com.quranapp.android.compose.components.AppBar
import com.quranapp.android.compose.components.IconButton
import com.quranapp.android.compose.components.Loader
import com.quranapp.android.compose.components.SimpleTooltip
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.utils.univ.formatted
import com.quranapp.android.utils.univ.toDate
import kotlinx.coroutines.launch

@Composable
fun AppLogsScreen() {
    val tabs = listOf(R.string.crashLogs, R.string.suppressedLogs)

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { AppBar(title = stringResource(R.string.appLogs)) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(stringResource(title)) },
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
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<AppLogModel>>(emptyList()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val files = Log.CRASH_LOGS_DIR.listFiles()

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

                    val parsedDate = logFile.name.toDate(Log.FILE_NAME_DATE_FORMAT)
                    val formattedDateTime = parsedDate?.formatted() ?: logFile.name

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
                text = stringResource(R.string.textNoLogsFound),
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

                MessageUtils.showRemovableToast(
                    context,
                    R.string.logRemoved,
                    Toast.LENGTH_SHORT,
                )
            }
        }
    }
}

@Composable
private fun SuppressedLogs() {
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<AppLogModel>>(emptyList()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val files = Log.SUPPRESSED_LOGS_DIR?.listFiles()

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

                    val parsedDate = datetimeStr.toDate(Log.FILE_NAME_DATE_FORMAT)
                    val formattedDateTime = parsedDate?.formatted() ?: logFile.name

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
                text = stringResource(R.string.textNoLogsFound),
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

                MessageUtils.showRemovableToast(
                    context,
                    R.string.logRemoved,
                    Toast.LENGTH_SHORT,
                )
            }
        }
    }
}

@Composable
private fun AppLogItem(log: AppLogModel, isCrash: Boolean, handleDelete: (AppLogModel) -> Unit) {
    val context = LocalContext.current

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
                SimpleTooltip(text = stringResource(R.string.strLabelDelete)) {
                    IconButton(
                        painter = painterResource(R.drawable.dr_icon_delete),
                        onClick = {
                            handleDelete(log)
                        },
                        tint = colorScheme.onSurface,
                        small = true,
                    )
                }
                SimpleTooltip(text = stringResource(R.string.strLabelCopy)) {
                    IconButton(
                        painter = painterResource(R.drawable.icon_copy),
                        onClick = {
                            context.copyToClipboard(log.log)
                            MessageUtils.showClipboardMessage(
                                context,
                                context.getString(R.string.copiedToClipboard),
                            )
                        },
                        tint = colorScheme.onSurface,
                        small = true,
                    )
                }

                SimpleTooltip(text = stringResource(R.string.github)) {
                    Button(
                        onClick = {
                            context.copyToClipboard(log.log)
                            MessageUtils.showRemovableToast(
                                context,
                                R.string.pasteCrashLogGithubIssue,
                                Toast.LENGTH_LONG,
                            )
                            AppBridge.newOpener(context)
                                .browseLink(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL)
                        },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(100),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.icon_github),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

