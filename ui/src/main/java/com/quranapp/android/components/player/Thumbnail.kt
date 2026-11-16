package com.quranapp.android.components.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.components.ChapterIcon
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.components.reader.navigator.ChapterVerseNavigator
import com.quranapp.android.core.R
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.locale.formattedStringResource
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.grayscaleColorFilter
import com.quranapp.android.theme.observeIsMonoPalette
import com.quranapp.android.viewmodel.RecitationPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExtendedThumbnail(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
    onOpenInReader: (() -> Unit)? = null,
) {
    val displayVerse = verse.takeIf { it.isValid } ?: ChapterVersePair(1, 1)
    val isMono = ThemeUtils.observeIsMonoPalette()
    val wallpaperFilter = remember { grayscaleColorFilter() }
    val headerShape = RoundedCornerShape(32.dp)
    val viewModel = appViewModel<RecitationPlayerViewModel>()

    var showChapterVerseNavigator by rememberSaveable { mutableStateOf(false) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(displayVerse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            viewModel.repository.getChapterName(displayVerse.chapterNo)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(headerShape)
            .background(if (isMono) Color(0xFF101010) else Color(0xFF10151C))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = headerShape
            )
    ) {
        Image(
            painter = painterResource(R.drawable.dr_quran_wallpaper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            colorFilter = if (isMono) wallpaperFilter else null,
        )

        ThumbnailScrim(isMono)

        val smallestAxis = minOf(maxWidth, maxHeight)
        val horizontalPadding = smallestAxis * 0.08f
        val verticalPadding = smallestAxis * 0.08f
        val chapterIconInset = smallestAxis * 0.07f
        val chapterIconBottomInset = smallestAxis * 0.025f
        val chapterIconSize = (smallestAxis.value * 0.2f).sp
        val titleToBadgeGap = smallestAxis * 0.025f
        val iconToTitleGap = smallestAxis * 0.06f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ChapterIcon(
                modifier = Modifier.padding(
                    start = chapterIconInset,
                    end = chapterIconInset,
                    top = chapterIconInset,
                    bottom = chapterIconBottomInset,
                ),
                chapterNo = displayVerse.chapterNo,
                fontSize = chapterIconSize,
                color = PlayerContentColor,
            )

            Spacer(Modifier.height(iconToTitleGap))

            Text(
                text = stringResource(R.string.strLabelSurah, chapterName),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (smallestAxis.value * 0.07f).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = PlayerContentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(titleToBadgeGap))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .clickable {
                        showChapterVerseNavigator = true
                    }
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp,
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedStringResource(
                            R.string.strLabelVerseNo,
                            displayVerse.verseNo
                        ),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        painterResource(R.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (onOpenInReader != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontalPadding, verticalPadding)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                    .clickable(
                        role = Role.Button,
                        onClick = onOpenInReader,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.dr_icon_read_quran),
                        contentDescription = stringResource(R.string.strLabelOpenInReader),
                        modifier = Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.88f),
                    )
                    Text(
                        text = stringResource(R.string.strLabelOpenInReader),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                    )
                }
            }
        }
    }

    ChapterVerseNavigator(
        isOpen = showChapterVerseNavigator,
        onDismiss = { showChapterVerseNavigator = false },
        selectedChapterNo = displayVerse.chapterNo,
        selectedVerseNos = setOf(displayVerse.verseNo),
        onVerseSelected = { chapterNo, verseNo ->
            viewModel.controller.start(ChapterVersePair(chapterNo, verseNo))
        },
    )
}

@Composable
fun LandscapePlayerIdentity(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val displayVerse = verse.takeIf { it.isValid } ?: ChapterVersePair(1, 1)
    val isMono = ThemeUtils.observeIsMonoPalette()
    val wallpaperFilter = remember { grayscaleColorFilter() }
    val viewModel = appViewModel<RecitationPlayerViewModel>()
    val artShape = RoundedCornerShape(22.dp)

    var showChapterVerseNavigator by rememberSaveable { mutableStateOf(false) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(displayVerse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            viewModel.repository.getChapterName(displayVerse.chapterNo)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val artSize = minOf(maxWidth, maxHeight) * 0.55f
        val itemGap = maxHeight * 0.03f

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(itemGap, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .shadow(18.dp, artShape)
                    .clip(artShape)
                    .background(if (isMono) Color(0xFF101010) else Color(0xFF10151C))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), artShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.dr_quran_wallpaper),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (isMono) wallpaperFilter else null,
                )

                ThumbnailScrim(isMono)

                ChapterIcon(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    chapterNo = displayVerse.chapterNo,
                    fontSize = (artSize.value * 0.22f).sp,
                    color = PlayerContentColor,
                )
            }

            Text(
                text = stringResource(R.string.strLabelSurah, chapterName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PlayerContentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .clickable { showChapterVerseNavigator = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formattedStringResource(
                            R.string.strLabelVerseNo,
                            displayVerse.verseNo
                        ),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        painterResource(R.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }

    ChapterVerseNavigator(
        isOpen = showChapterVerseNavigator,
        onDismiss = { showChapterVerseNavigator = false },
        selectedChapterNo = displayVerse.chapterNo,
        selectedVerseNos = setOf(displayVerse.verseNo),
        onVerseSelected = { chapterNo, verseNo ->
            viewModel.controller.start(ChapterVersePair(chapterNo, verseNo))
        },
    )
}

@Composable
private fun BoxScope.ThumbnailScrim(isMono: Boolean) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.78f),
                        Color.Black.copy(alpha = 0.55f),
                    )
                )
            )
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.28f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.22f),
                    )
                )
            )
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isMono) Color.White.copy(alpha = 0.08f)
                        else colorScheme.primary.copy(alpha = 0.2f),
                        Color.Transparent,
                        Color.Transparent,
                    )
                )
            )
    )
}

@Composable
fun MiniPlayerThumbnail(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val isMono = ThemeUtils.observeIsMonoPalette()
    val wallpaperFilter = remember { grayscaleColorFilter() }

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .background(if (isMono) Color(0xFF101010) else Color(0xFF11161D))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.dr_quran_wallpaper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            colorFilter = if (isMono) wallpaperFilter else null,
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isMono) Color.White.copy(alpha = 0.1f)
                            else colorScheme.primary.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
        )

        ChapterIcon(
            modifier = Modifier.padding(top = 4.dp),
            chapterNo = verse.chapterNo,
            fontSize = 24.sp,
            color = PlayerContentColor,
            withPrefix = false
        )
    }
}
