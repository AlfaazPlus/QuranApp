package com.quranapp.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.db.entities.mapToUiModel
import com.quranapp.android.ui.components.common.BoldHeader
import com.quranapp.android.ui.components.common.DeleteButton
import com.quranapp.android.ui.components.ReadHistory.ReadHistoryItem
import com.quranapp.android.viewModels.ReadHistoryViewModel

@Composable
fun ReadHistoryScreen() {
    val context = LocalContext.current
    val readHistoryViewModel: ReadHistoryViewModel = viewModel()
    readHistoryViewModel.init(context)

    val history by readHistoryViewModel.history.collectAsState()
    val quranMeta by readHistoryViewModel.quranMeta.collectAsState()

    Scaffold(
        topBar = {
            BoldHeader(
                text = stringResource(id = R.string.strTitleReadHistory),
                deleteButton = {
                    if (history.isNotEmpty()) {
                        DeleteButton(
                            imageDescription = stringResource(id = R.string.msgClearReadHistory),
                            dialogTitle = stringResource(id = R.string.msgClearReadHistory),
                            dialogText = if (history.size > 1) stringResource(R.string.nItems, history.size) else stringResource(R.string.nItem, history.size),
                            deleteButtonText = stringResource(id = R.string.strLabelRemoveAll),
                            onDelete = { readHistoryViewModel.deleteAllHistory() }
                        )
                    }
                }
            )
        }
    ) { pv ->

        Column(
            modifier = Modifier
                .padding(pv)
                .background(colorResource(id = R.color.colorBGPageVariable))
        ) {

            if (readHistoryViewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(40.dp),
                        color = colorResource(id = R.color.colorPrimary)
                    )
                }
            } else {
                ReadHistoryList(history = history.map { it.mapToUiModel() }, quranMeta = quranMeta)
            }

        }
    }

}


@Composable
fun ReadHistoryList(
    modifier: Modifier = Modifier,
    history: List<ReadHistoryModel>,
    quranMeta: QuranMeta
) {
    if (history.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.colorBGPage)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.dr_icon_history),
                contentDescription = stringResource(id = R.string.strMsgReadHistoryNoItems),
                modifier = Modifier
                    .height(100.dp)
                    .width(100.dp)
            )
            Text(
                text = stringResource(id = R.string.strMsgReadHistoryNoItems),
                color = colorResource(id = R.color.colorText3),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(0.dp, 10.dp, 10.dp, 50.dp)
        ) {
            items(history) {
                ReadHistoryItem(historyItem = it, quranMeta = quranMeta)
            }
        }
    }

}

