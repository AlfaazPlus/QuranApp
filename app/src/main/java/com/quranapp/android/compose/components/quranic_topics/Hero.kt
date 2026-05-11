package com.quranapp.android.compose.components.quranic_topics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.quranapp.android.compose.theme.fontArabic
import com.quranapp.android.R
import com.quranapp.android.api.resolveInventoryUrl
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.extensions.bottomBorder
import com.quranapp.android.compose.screens.quranictopics.QuranicTopicRoutes
import com.quranapp.android.compose.screens.quranictopics.topicsNavOptions
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.TopicNode
import com.quranapp.android.viewModels.TopicsTree
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader


@Composable
internal fun TopicHeroCard(
    navController: NavController,
    topic: TopicNode,
    tree: TopicsTree,
    breadcrumbs: List<TopicNode>,
    visibleRelatedCount: Int,
) {
    val context = LocalContext.current
    val downloadSource = AppPreferences.observeResourceDownloadProxy()
    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }

    val heroImageData = remember(topic.imageUrl, downloadSource) {
        topic.imageUrl?.let(::resolveInventoryUrl) ?: R.drawable.quran_wallpaper
    }

    val heroImageModel = remember(heroImageData, context) {
        ImageRequest.Builder(context)
            .data(heroImageData)
            .crossfade(true)
            .build()
    }

    val kindLabel = topic.kindLabel()
    val typeLabel = topic.type.readableType()

    Box(Modifier.padding(bottom = 8.dp)) {
        Column(
            Modifier
                .background(colorScheme.surfaceContainer)
                .bottomBorder(1.dp, colorScheme.outlineVariant.alpha(0.55f)),
        ) {
            if (breadcrumbs.isNotEmpty()) {
                BreadcrumbTrail(
                    rootLabel = if (tree == TopicsTree.Ontology) "Ontology" else "Themes",
                    breadcrumbs = breadcrumbs,
                    currentTopic = topic,
                    onRootClick = {
                        navController.navigate(
                            if (tree == TopicsTree.Ontology) {
                                QuranicTopicRoutes.ONTOLOGY
                            } else {
                                QuranicTopicRoutes.THEMATIC
                            },
                            topicsNavOptions {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        )
                    },
                    onBreadcrumbClick = { breadcrumb, index ->
                        navController.navigate(
                            QuranicTopicRoutes.topic(
                                tree = tree,
                                topicId = breadcrumb.id,
                                trail = breadcrumbs.take(index).map { it.id },
                            ),
                            topicsNavOptions()
                        )
                    },
                )
            }

            Surface(
                modifier = Modifier.padding(12.dp),
                shape = shapes.medium,
            ) {
                AsyncImage(
                    model = heroImageModel,
                    contentDescription = topic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                )
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TypePill(topic.kindLabel())

                    if (typeLabel != kindLabel) {
                        TypePill(typeLabel)
                    }
                }

                val description = topic.shortDescription ?: topic.description ?: ""

                if (description.isNotEmpty()) {
                    TopicDescriptionRichText(
                        description = description,
                        onTopicClick = { targetTopicId ->
                            navController.navigate(
                                QuranicTopicRoutes.topic(
                                    tree = tree,
                                    topicId = targetTopicId,
                                    trail = breadcrumbs.map { it.id } + topic.id,
                                )
                            )
                        },
                        onReferenceClick = { chapterNo, verses ->
                            quickRefData = QuickReferenceData(
                                slugs = emptySet(),
                                chapterNo = chapterNo,
                                verses = verses,
                            )
                        },
                    )
                }

                TopicStatsRow(
                    topic = topic,
                    visibleRelatedCount = visibleRelatedCount,
                )
            }
        }

        QuickReference(
            data = quickRefData,
            onOpenInReader = { chapterNo, range ->
                quickRefData = null
                ReaderFactory.startVerseRange(context, chapterNo, range.first, range.last)
            },
            onClose = { quickRefData = null },
        )
    }
}

@Composable
private fun TopicDescriptionRichText(
    description: String,
    onTopicClick: (Int) -> Unit,
    onReferenceClick: (chapterNo: Int, verses: String) -> Unit,
) {
    val annotated = remember(description) {
        parseTopicDescription(
            input = description,
            onTopicClick = onTopicClick,
            onReferenceClick = onReferenceClick,
        )
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = colorScheme.onSurface,
        ),
    )
}

private data class OpenTag(
    val name: String,
    val pushedStyle: Boolean = false,
    val pushedLink: Boolean = false,
)

private fun parseTopicDescription(
    input: String,
    onTopicClick: (Int) -> Unit,
    onReferenceClick: (chapterNo: Int, verses: String) -> Unit,
): AnnotatedString {
    return try {
        val builder = AnnotatedString.Builder()
        val stack = ArrayDeque<OpenTag>()
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(StringReader("<root>$input</root>"))
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name.lowercase()
                    if (tagName == "root") {
                        stack.addLast(OpenTag(name = tagName))
                    } else {
                        when (tagName) {
                            "b" -> {
                                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                stack.addLast(OpenTag(name = tagName, pushedStyle = true))
                            }

                            "ar" -> {
                                builder.pushStyle(SpanStyle(fontFamily = fontArabic))
                                stack.addLast(OpenTag(name = tagName, pushedStyle = true))
                            }

                            "topic" -> {
                                val topicId = parser.getAttributeValue(null, "id")
                                    ?: parser.getAttributeValue("", "id")

                                if (!topicId.isNullOrBlank()) {
                                    val topicInt = topicId.toIntOrNull()

                                    builder.pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = "topic:$topicId",
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            ),
                                        ) {
                                            if (topicInt != null) onTopicClick(topicInt)
                                        }
                                    )
                                    stack.addLast(OpenTag(name = tagName, pushedLink = true))
                                } else {
                                    stack.addLast(OpenTag(name = tagName))
                                }
                            }

                            "reference" -> {
                                val chapter = parser.getAttributeValue(null, "chapter")
                                    ?: parser.getAttributeValue("", "chapter")
                                if (!chapter.isNullOrBlank()) {
                                    val chapterInt = chapter.toIntOrNull()
                                    val verses = parser.getAttributeValue(null, "verses")
                                        ?: parser.getAttributeValue("", "verses")
                                        ?: ""
                                    builder.pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = "reference:$chapter|$verses",
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            ),
                                        ) {
                                            if (chapterInt != null) onReferenceClick(
                                                chapterInt,
                                                verses
                                            )
                                        }
                                    )
                                    stack.addLast(OpenTag(name = tagName, pushedLink = true))
                                } else {
                                    stack.addLast(OpenTag(name = tagName))
                                }
                            }

                            else -> {
                                // Unknown tag: ignore wrapper and keep plain inner text.
                                stack.addLast(OpenTag(name = tagName))
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val closeTag = parser.name.lowercase()
                    while (stack.isNotEmpty()) {
                        val open = stack.removeLast()
                        if (open.pushedStyle) builder.pop()
                        if (open.pushedLink) builder.pop()
                        if (open.name == closeTag) break
                    }
                }

                XmlPullParser.TEXT -> {
                    builder.append(parser.text.orEmpty())
                }
            }
            eventType = parser.next()
        }

        while (stack.isNotEmpty()) {
            val open = stack.removeLast()
            if (open.pushedStyle) builder.pop()
            if (open.pushedLink) builder.pop()
        }

        builder.toAnnotatedString()
    } catch (e: Exception) {
        Log.saveError(e, "parseTopicDescription")
        AnnotatedString(stripTagsFallback(input))
    }
}


private fun stripTagsFallback(input: String): String {
    if (input.isEmpty()) return input
    val out = StringBuilder(input.length)
    var inTag = false
    input.forEach { ch ->
        when (ch) {
            '<' -> inTag = true
            '>' -> inTag = false
            else -> if (!inTag) out.append(ch)
        }
    }
    return out.toString()
}


@Composable
private fun TopicStatsRow(
    topic: TopicNode,
    visibleRelatedCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            label = "Subtopics",
            value = topic.childCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Verses",
            value = topic.verseCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Related",
            value = visibleRelatedCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
