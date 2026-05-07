package com.quranapp.android.compose.screens.quranictopics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.screens.quranictopics.QuranicTopicRoutes
import com.quranapp.android.viewModels.QuranicTopicNode
import com.quranapp.android.viewModels.QuranicTopicsTree

@Composable
internal fun QuranicTopicListRoute(
    title: String,
    tree: QuranicTopicsTree,
    isLoading: Boolean,
    topics: List<QuranicTopicNode>,
    navController: NavController,
) {
    var infoDialogShown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            AppBar(
                title = title,
                actions = {
                    IconButton(
                        painter = painterResource(R.drawable.dr_icon_info)
                    ) {
                        infoDialogShown = true
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading -> Loader(true)
            topics.isEmpty() -> EmptyContent(
                modifier = Modifier.padding(padding),
                message = "No topics found",
            )

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = 64.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        ListIntroCard(
                            tree = tree,
                            count = topics.size,
                        )
                    }

                    items(topics, key = { it.id }) { topic ->
                        TopicListItem(
                            topic = topic,
                            accent = if (tree == QuranicTopicsTree.Ontology) {
                                colorScheme.primary
                            } else {
                                colorScheme.tertiary
                            },
                            onClick = {
                                navController.navigate(
                                    QuranicTopicRoutes.topic(
                                        tree = tree,
                                        topicId = topic.id,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    AlertDialog(
        isOpen = infoDialogShown,
        onClose = { infoDialogShown = false },
        title = stringResource(R.string.about_this_page),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelGotIt),
            ),
        ),
    ) {
        Text(
            stringResource(R.string.englishContentOnly)
        )
    }
}
