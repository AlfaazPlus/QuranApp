package com.quranapp.android.ui.components.homepage.readHistory

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReadHistory
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.db.entities.mapToUiModel
import com.quranapp.android.ui.components.common.SectionHeader
import com.quranapp.android.viewModels.ReadHistoryViewModel

@Composable
fun ReadHistorySection() {
    val context = LocalContext.current
    val readHistoryViewModel: ReadHistoryViewModel = viewModel()
    readHistoryViewModel.init(context)

    val history by  readHistoryViewModel.history.collectAsState()
    val quranMeta by readHistoryViewModel.quranMeta.collectAsState()

    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(colorResource(id = R.color.colorBGHomePageItem))
    ) {
        SectionHeader(
            icon = R.drawable.dr_icon_history,
            iconColor = R.color.colorPrimary,
            title =  R.string.strTitleReadHistory
        ) {
            context.startActivity(Intent(context, ActivityReadHistory::class.java))
        }

        if (readHistoryViewModel.isLoading){
            Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp), contentAlignment = Alignment.Center){
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = colorResource(id = R.color.colorPrimary)
                )
            }
        }else{
            ReadHistoryList(history = history.map { it.mapToUiModel() }, quranMeta = quranMeta)
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
        Text(
            text = stringResource(id = R.string.strMsgReadShowupHere),
            fontSize = dimensionResource(id = R.dimen.dmnCommonSize1_5).value.sp,
            color = colorResource(id = R.color.colorText),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            modifier = modifier
                    .fillMaxWidth()
                    .padding(20.dp, 15.dp)
        )
    } else {
        val itemsList = if(history.size > 10) history.subList(0, 10) else history
        LazyRow (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(15.dp)
        ){
            items(itemsList) {
                ReadHistoryItem(historyItem = it, quranMeta = quranMeta)
            }
        }
    }

}

