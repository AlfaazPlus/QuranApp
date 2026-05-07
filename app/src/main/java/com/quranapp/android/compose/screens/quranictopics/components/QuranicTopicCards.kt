package com.quranapp.android.compose.screens.quranictopics.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.alfaazplus.sunnah.ui.theme.fontArabic
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.extensions.bottomBorder
import com.quranapp.android.compose.screens.quranictopics.QuranicTopicRoutes
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.repository.TopicVersePreview
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.quran.parser.ParserUtils
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.viewModels.QuranicTopicNode
import com.quranapp.android.viewModels.QuranicTopicRelationship
import com.quranapp.android.viewModels.QuranicTopicsTree
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@Composable
internal fun ListIntroCard(
    tree: QuranicTopicsTree,
    count: Int,
) {
    val title = when (tree) {
        QuranicTopicsTree.Ontology -> "Browse from general to specific"
        QuranicTopicsTree.Thematic -> "Browse by meaning and theme"
    }

    val body = when (tree) {
        QuranicTopicsTree.Ontology -> "Open a category to move gradually into focused concepts."
        QuranicTopicsTree.Thematic -> "Themes organize meanings people usually explore together."
    }

    Column(
        Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurface.alpha(0.7f),
        )

        Text(
            text = plural(count, "starting point"),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun TopicListItem(
    topic: QuranicTopicNode,
    accent: Color,
    onClick: () -> Unit,
) {
    val kindLabel = topic.kindLabel()
    val typeLabel = topic.type.readableType()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = shapes.medium,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopicIcon(
                label = topic.icon ?: topic.title.take(1),
                background = accent.copy(alpha = 0.18f),
                contentColor = colorScheme.onSurface,
                size = 42,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TypePill(topic.kindLabel())

                    if (typeLabel != kindLabel) {
                        TypePill(typeLabel)
                    }
                }
            }

            NodeCount(topic = topic)

            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun TopicExploreCard(
    topic: QuranicTopicNode,
    hasVerses: Boolean,
    hasSubtopics: Boolean,
    hasRelated: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shapes.medium,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExploreLine(
                title = "Explore this topic",
                body = topic.explorationHint(hasVerses, hasSubtopics, hasRelated),
            )

            if (hasVerses) {
                ExploreLine(
                    title = "Read the verses",
                    body = "See how this meaning appears in its Qur'anic references.",
                )
            }

            if (hasSubtopics) {
                ExploreLine(
                    title = "Go deeper",
                    body = "Open a subtopic to continue in a narrower direction.",
                )
            }

            if (hasRelated) {
                ExploreLine(
                    title = "Follow related concepts",
                    body = "Move sideways to linked ideas for broader reflection.",
                )
            }
        }
    }
}

@Composable
private fun ExploreLine(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.alpha(0.8f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
internal fun VerseRefsCard(
    topic: QuranicTopicNode,
    totalCount: Int,
    verseRefs: List<String>,
    previews: List<TopicVersePreview>,
) {
    val context = LocalContext.current


    fun openTopicReference(
        context: Context,
        topic: QuranicTopicNode,
    ) {
        val compressedRefs = ParserUtils.compressVerseRefsByChapter(verseRefs)
        val chapters = compressedRefs
            .mapNotNull { it.substringBefore(':').toIntOrNull() }
            .toSet()

        val description = topic.shortDescription ?: ""

        val intent = ReaderFactory.prepareReferenceVerseIntent(
            title = topic.title,
            desc = StringUtils.removeHTML(description, true),
            translSlug = emptySet(),
            chapters = chapters,
            verses = compressedRefs.toSet(),
        ).apply {
            setClass(context, ActivityReference::class.java)
        }

        context.startActivity(intent)
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shapes.medium)
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant.copy(0.45f), shapes.medium)
            .clickable {
                openTopicReference(context, topic)
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = "Verses ($totalCount)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )

            Text(
                stringResource(R.string.strLabelViewAll),
                style = typography.labelMedium
            )
        }

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previews.forEachIndexed { index, preview ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = shapes.small,
                    color = colorScheme.background,
                    border = BorderStroke(
                        1.dp,
                        colorScheme.outlineVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(
                        Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = formattedStringResource(
                                R.string.strLabelVerseSerial,
                                preview.chapterNo,
                                preview.verseNo
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(colorScheme.surface)
                                .clickable(
                                    onClick = {
                                        context.copyToClipboard("${preview.chapterNo}:${preview.verseNo}")
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = colorScheme.onBackground,
                            style = typography.labelMedium,
                        )

                        val translationText = StringUtils.removeHTML(preview.translation, false)

                        if (translationText.isNotEmpty()) {
                            Text(
                                text = translationText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            if (totalCount > previews.size) {
                Text(
                    text = "+${totalCount - previews.size} more verses",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun RelationshipItem(
    relationship: QuranicTopicRelationship,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = relationship.type.readableLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )

        TopicListItem(
            topic = relationship.topic,
            accent = when (relationship.type) {
                RelationshipType.RELATED -> colorScheme.secondary
                RelationshipType.THEMATIC_PARENT -> colorScheme.tertiary
                else -> colorScheme.primary
            },
            onClick = onClick,
        )
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
        )
    }
}
