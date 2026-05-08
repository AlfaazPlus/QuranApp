package com.quranapp.android.compose.components.quranic_topics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.viewModels.TopicNode
import java.util.Locale

@Composable
internal fun EmptyContent(
    modifier: Modifier = Modifier,
    message: String,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
internal fun TopicIcon(
    label: String?,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    size: Int = 44,
) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size((size * 0.46f).dp),
                )
            } else {
                Text(
                    text = label.orEmpty().take(2),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun NodeCount(topic: TopicNode) {
    val labels = buildList {
        if (topic.childCount > 0) add(plural(topic.childCount, "subtopic"))
        if (topic.verseCount > 0) add(plural(topic.verseCount, "verse"))
    }

    if (labels.isEmpty()) return

    Column(
        horizontalAlignment = Alignment.End,
    ) {
        labels.take(2).forEach { label ->
            CountPill(label)
        }
    }
}

@Composable
private fun CountPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun TypePill(
    text: String,
) {
    Surface(
        shape = shapes.large,
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)),
        tonalElevation = 4.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

internal fun plural(count: Int, singular: String): String {
    val suffix = if (count == 1) singular else "${singular}s"
    return "$count $suffix"
}

internal fun TopicNode.kindLabel(): String =
    when {
        childCount > 0 && verseCount > 0 -> "Hybrid"
        childCount > 0 -> "Category"
        verseCount > 0 -> "Topic"
        else -> "Node"
    }

internal fun String.readableType(): String =
    replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.getDefault())
                } else {
                    it.toString()
                }
            }
        }

internal fun RelationshipType.readableLabel(): String =
    when (this) {
        RelationshipType.RELATED -> "Related concept"
        RelationshipType.PARENT -> "Parent connection"
        RelationshipType.THEMATIC_PARENT -> "Theme connection"
        RelationshipType.ONTOLOGY_PARENT -> "Ontology connection"
        RelationshipType.NONE -> "Connection"
    }

internal fun String.asPreviewText(): String =
    replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun TopicNode.explorationHint(
    hasVerses: Boolean,
    hasSubtopics: Boolean,
    hasRelated: Boolean,
): String =
    when {
        hasVerses && hasSubtopics -> "Begin with the verses, then move into the subtopics for deeper study."
        hasVerses -> "This topic is verse-focused. Read references in sequence for better context."
        hasSubtopics -> "This is a broad topic. Open a subtopic to narrow your study."
        hasRelated -> "Use related concepts to continue exploring nearby ideas."
        else -> "This topic is present, but linked study material is still limited."
    }
