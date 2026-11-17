package com.quranapp.android.screens.reference

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.components.ReferenceThumbnail
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.BottomSheetMenu
import com.quranapp.android.components.common.BottomSheetMenuItem
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.alphabetically
import com.quranapp.android.generated.resources.chronological
import com.quranapp.android.generated.resources.dr_icon_sort
import com.quranapp.android.generated.resources.sortBy
import com.quranapp.android.generated.resources.strHintSearchProphet
import com.quranapp.android.generated.resources.strMsgDuaOf
import com.quranapp.android.generated.resources.strMsgPropheticDuaInQuran
import com.quranapp.android.generated.resources.strMsgReferenceDuas
import com.quranapp.android.generated.resources.strTitleFeaturedDuas
import com.quranapp.android.generated.resources.strTitleFilters
import com.quranapp.android.locale.rememberLocalized
import com.quranapp.android.models.PropheticDuaProphet
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.alpha
import com.quranapp.android.theme.grayscaleColorFilter
import com.quranapp.android.theme.observeIsMonoPalette
import com.quranapp.android.utils.quran.parser.QuranPropheticDuasParser
import com.quranapp.android.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.text.MessageFormat
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PropheticDuasScreen(title: String? = null) {
    val allProphets = rememberLocalized { QuranPropheticDuasParser.parsePropheticDuas(it) }

    val barTitle = title?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.strTitleFeaturedDuas)

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(150.milliseconds)
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
                pattern.matcher(prophet.name + prophet.order).find()
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = barTitle,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = stringResource(Res.string.strHintSearchProphet),
                actions = {
                    val sortLabel = stringResource(Res.string.strTitleFilters)
                    SimpleTooltip(text = sortLabel) {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_sort),
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
                        .padding(paddingValues),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            displayedProphets,
                            key = { "${it.name}_${it.order}" },
                        ) { prophet ->
                            PropheticDuaListItem(prophet = prophet)
                        }
                    }
                }
            }
        }
    }

    BottomSheetMenu(
        isOpen = sortMenuOpen,
        onDismiss = { sortMenuOpen = false },
        icon = Res.drawable.dr_icon_sort,
        title = stringResource(Res.string.sortBy),
    ) {
        BottomSheetMenuItem(
            text = stringResource(Res.string.alphabetically),
            isSelected = sortMode == 0,
            onClick = {
                sortMode = 0
                sortMenuOpen = false
            },
        )
        BottomSheetMenuItem(
            text = stringResource(Res.string.chronological),
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
private fun PropheticDuaListItem(prophet: PropheticDuaProphet) {
    val context = LocalContext.current
    val isMono = ThemeUtils.observeIsMonoPalette()
    val grayscale = remember { grayscaleColorFilter() }

    val nameLine = MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
    val scope = rememberCoroutineScope()

    Card(
        onClick = {
            scope.launch {
                val refTitle = getString(
                    Res.string.strMsgPropheticDuaInQuran,
                    MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific),
                )

                ReaderFactory.startReferenceVerse(
                    context,
                    ReferenceVerseModel(
                        title = refTitle,
                        desc = getString(Res.string.strMsgReferenceDuas),
                        chapters = prophet.chapters,
                        verses = prophet.verses,
                        thumbnail = prophet.thumbnail?.let { ReferenceThumbnail.RemoteUrl(it) }
                    )
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(prophet.icon),
                contentDescription = prophet.name,
                modifier = Modifier.size(56.dp),
                colorFilter = if (isMono) grayscale else null,
            )

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.strMsgDuaOf, nameLine),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                prophet.inChapters?.takeIf { it.isNotBlank() }?.let { chapters ->
                    Text(
                        text = chapters,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
