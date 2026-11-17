package com.quranapp.android.screens.collections
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.quranapp.android.generated.resources.*
import com.quranapp.android.generated.resources.Res

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.collections.collectionColor
import com.quranapp.android.components.collections.collectionIcon
import com.quranapp.android.db.entities.user.CollectionWithCount
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun CollectionCard(
    item: CollectionWithCount,
    onClick: () -> Unit,
) {
    val collection = item.collection
    val color = collectionColor(collection.color)

    Surface(
        onClick = onClick,
        shape = shapes.medium,
        color = colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(shapes.large)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(collectionIcon(collection.icon_key)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = pluralStringResource(
                        Res.plurals.verse_count,
                        item.verseCount,
                        item.verseCount,
                    ),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )

                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = updatedLabel(collection.updatedAt),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
fun updatedLabel(updatedAt: Long): String {
    var nowMillis by remember(updatedAt) { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }

    LaunchedEffect(updatedAt) {
        while (true) {
            val elapsed = (Clock.System.now().toEpochMilliseconds() - updatedAt).coerceAtLeast(0)
            val wait = when {
                elapsed < 60_000L -> 60_000L - elapsed
                elapsed < 3_600_000L -> 60_000L
                else -> 3_600_000L
            }
            delay(wait.coerceAtLeast(1_000L).milliseconds)
            nowMillis = Clock.System.now().toEpochMilliseconds()
        }
    }

    val delta = (nowMillis - updatedAt).coerceAtLeast(0)
    val minutes = (delta / 60_000L).toInt()
    val hours = (delta / 3_600_000L).toInt()
    val days = (delta / 86_400_000L).toInt()

    return when {
        minutes < 1 -> stringResource(Res.string.updated_just_now)
        minutes < 60 -> pluralStringResource(Res.plurals.updated_minutes, minutes, minutes)
        hours < 24 -> pluralStringResource(Res.plurals.updated_hours, hours, hours)
        else -> pluralStringResource(
            Res.plurals.updated_days,
            days.coerceAtLeast(1),
            days.coerceAtLeast(1),
        )
    }
}
