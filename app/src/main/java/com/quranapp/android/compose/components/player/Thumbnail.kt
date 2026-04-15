package com.quranapp.android.compose.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExtendedThumbnail(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val headerShape = RoundedCornerShape(32.dp)

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(verse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(verse.chapterNo)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(headerShape)
            .background(Color(0xFF10151C))
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
        )

        // Deep tint
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
                            Color.Black.copy(alpha = 0.22f)
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
                            colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )

        val smallestAxis = minOf(maxWidth, maxHeight)
        val compact = smallestAxis <= 190.dp
        val medium = smallestAxis <= 250.dp

        val horizontalPadding: Dp = when {
            compact -> 14.dp
            medium -> 18.dp
            else -> 24.dp
        }
        val verticalPadding: Dp = when {
            compact -> 14.dp
            medium -> 18.dp
            else -> 28.dp
        }
        val chapterIconInset: Dp = when {
            compact -> 10.dp
            medium -> 16.dp
            else -> 24.dp
        }
        val chapterIconBottomInset: Dp = when {
            compact -> 4.dp
            else -> 8.dp
        }

        val chapterIconSize = (smallestAxis.value * 0.2f)
            .coerceIn(44f, 72f)
            .sp

        val titleStyle = when {
            compact -> MaterialTheme.typography.titleMedium
            medium -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.headlineSmall
        }
        val titleToBadgeGap: Dp = if (compact) 4.dp else 6.dp
        val iconToTitleGap: Dp = when {
            compact -> 10.dp
            medium -> 14.dp
            else -> 22.dp
        }
        val badgeHorizontalPadding: Dp = if (compact) 10.dp else 14.dp
        val badgeVerticalPadding: Dp = if (compact) 4.dp else 6.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(headerShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), headerShape)
            ) {
                ChapterIcon(
                    modifier = Modifier.padding(
                        start = chapterIconInset,
                        end = chapterIconInset,
                        top = chapterIconInset,
                        bottom = chapterIconBottomInset,
                    ),
                    chapterNo = verse.chapterNo,
                    fontSize = chapterIconSize,
                    color = PlayerContentColor,
                )
            }

            Spacer(Modifier.height(iconToTitleGap))

            Text(
                text = stringResource(R.string.strLabelSurah, chapterName),
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = PlayerContentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(titleToBadgeGap))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.1f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Text(
                    text = stringResource(R.string.strLabelVerseNo, verse.verseNo),
                    modifier = Modifier.padding(
                        horizontal = badgeHorizontalPadding,
                        vertical = badgeVerticalPadding,
                    ),
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun MiniPlayerThumbnail(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .background(Color(0xFF11161D))
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
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.3f),
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