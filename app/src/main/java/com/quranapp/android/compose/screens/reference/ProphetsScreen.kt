package com.quranapp.android.compose.screens.reference

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.BottomSheetMenu
import com.quranapp.android.compose.components.common.BottomSheetMenuItem
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareReferenceVerseIntent
import kotlinx.coroutines.delay
import java.text.MessageFormat
import java.util.regex.Pattern

@Composable
fun ProphetsScreen() {
    val context = LocalContext.current
    val allProphets = QuranProphet.observe()

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(150)
        debouncedQuery = searchQuery
    }

    var sortMode by remember { mutableIntStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val displayedProphets = remember(debouncedQuery, sortMode, allProphets) {
        val list = allProphets ?: return@remember emptyList()
        val q = debouncedQuery.trim()
        if (q.isEmpty()) {
            when (sortMode) {
                0 -> list.sortedBy { it.name }
                else -> list.sortedBy { it.order }
            }
        } else {
            val pattern =
                Pattern.compile(q, Pattern.CASE_INSENSITIVE or Pattern.LITERAL or Pattern.DOTALL)
            list.filter { prophet ->
                pattern.matcher(prophet.name + prophet.nameEn + prophet.nameAr).find()
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.strTitleProphets),
                bgColor = colorResource(R.color.colorBGHomePageItem),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = stringResource(R.string.strHintSearchProphet),
                actions = {
                    val sortLabel = stringResource(R.string.strTitleFilters)
                    SimpleTooltip(text = sortLabel) {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_sort),
                                contentDescription = sortLabel,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            allProphets == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (maxWidth < 600.dp) 1 else 2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 64.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            displayedProphets,
                            key = { "${it.name}_${it.order}_${it.nameEn}" },
                        ) { prophet ->
                            ProphetListItem(prophet = prophet)
                        }
                    }
                }
            }
        }
    }

    BottomSheetMenu(
        isOpen = sortMenuOpen,
        onDismiss = { sortMenuOpen = false },
        icon = R.drawable.dr_icon_sort,
        title = stringResource(R.string.sortBy),
    ) {
        BottomSheetMenuItem(
            text = stringResource(R.string.alphabetically),
            isSelected = sortMode == 0,
            onClick = {
                sortMode = 0
                sortMenuOpen = false
            },
        )
        BottomSheetMenuItem(
            text = stringResource(R.string.chronological),
            isSelected = sortMode == 1,
            onClick = {
                sortMode = 1
                sortMenuOpen = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProphetListItem(prophet: QuranProphet.Prophet) {
    val context = LocalContext.current
    val resources = LocalResources.current

    Card(
        onClick = {
            val title = resources.getString(
                R.string.strMsgReferenceInQuran,
                MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific),
            )

            val desc = resources.getString(
                R.string.strMsgReferenceFoundPlaces,
                title,
                prophet.verses.size,
            )

            val intent = prepareReferenceVerseIntent(
                true,
                title,
                desc,
                arrayOf(),
                prophet.chapters,
                prophet.verses,
            ).apply {
                setClass(context, ActivityReference::class.java)
            }
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = prophet.iconRes),
                contentDescription = prophet.name,
                modifier = Modifier.size(56.dp),
            )

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth(),
            ) {
                val titleLine = MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
                Text(
                    text = titleLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "English : ${prophet.nameEn}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                )
                prophet.inChapters?.takeIf { it.isNotBlank() }?.let { chapters ->
                    Text(
                        text = chapters,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.colorText2),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
