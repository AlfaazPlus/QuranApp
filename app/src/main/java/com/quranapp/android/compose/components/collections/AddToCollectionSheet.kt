package com.quranapp.android.compose.components.collections

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.SearchTextField
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.user.CollectionWithCount
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.CollectionsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionSheet(
    chapterNo: Int?,
    verseNo: Int?,
    onDismiss: () -> Unit,
) {
    var showCreateCollectionSheet by remember { mutableStateOf(false) }

    CreateUpdateCollectionSheet(
        isOpen = showCreateCollectionSheet,
        onClose = { showCreateCollectionSheet = false },
    )

    BottomSheet(
        isOpen = chapterNo != null && verseNo != null,
        onDismiss = onDismiss,
        title = stringResource(R.string.add_to_collection),
        actions = {
            TextButton(onClick = { showCreateCollectionSheet = true }) {
                Text(stringResource(R.string.create))
            }
        },
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
    ) {
        if (chapterNo != null && verseNo != null) {
            Content(chapterNo, verseNo, onDismiss)
        }
    }
}

@Composable
private fun Content(
    chapterNo: Int,
    verseNo: Int,
    onDismiss: () -> Unit,
) {
    val collectionVm = viewModel<CollectionsViewModel>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DatabaseProvider.getUserRepository(context) }
    var query by remember(chapterNo, verseNo) { mutableStateOf("") }
    var debouncedQuery by remember(chapterNo, verseNo) { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(300.milliseconds)
        debouncedQuery = query
    }

    val collections = remember(debouncedQuery) {
        collectionVm.collections(debouncedQuery)
    }.collectAsLazyPagingItems()
    var savedMembership by remember(chapterNo, verseNo) { mutableStateOf<Set<Long>?>(null) }
    var selected by remember(chapterNo, verseNo) { mutableStateOf<Set<Long>?>(null) }

    LaunchedEffect(chapterNo, verseNo) {
        repository.observeCollectionIdsForVerse(chapterNo, verseNo).collect { ids ->
            val membership = ids.toSet()
            if (selected == null) selected = membership
            savedMembership = membership
        }
    }

    val current = selected ?: savedMembership ?: emptySet()
    val unchanged = savedMembership == null || current == savedMembership

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        SearchTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = stringResource(R.string.strHintSearch),
        )

        if (savedMembership == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Loader()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                items(
                    count = collections.itemCount,
                    key = { index ->
                        collections.peek(index)?.collection?.id?.let { "col-$it" }
                            ?: "col-index-$index"
                    },
                ) { index ->
                    val item = collections[index] ?: return@items
                    val membership = savedMembership ?: return@items

                    CollectionPickRow(
                        item = item,
                        checked = item.collection.id in current,
                        onToggle = {
                            val id = item.collection.id
                            val base = selected ?: membership
                            selected = if (id in base) base - id else base + id
                        },
                    )
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    repository.applyVerseMembership(chapterNo, verseNo, current)

                    MessageUtils.showRemovableToast(
                        context,
                        R.string.collection_updated,
                        Toast.LENGTH_SHORT,
                    )

                    onDismiss()
                }
            },
            enabled = !unchanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.strLabelUpdate))
        }
    }
}

@Composable
private fun CollectionPickRow(
    item: CollectionWithCount,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })

        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = collectionColor(item.collection.color),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(collectionIcon(item.collection.icon_key)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = item.collection.name,
                style = typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.verse_count,
                    item.verseCount,
                    item.verseCount,
                ),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}
