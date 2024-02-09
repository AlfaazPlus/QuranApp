package com.quranapp.android.ui.components.ReadHistory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.factory.ReaderFactory

@Composable
fun ReadHistoryItem(
    modifier: Modifier = Modifier,
    historyItem: ReadHistoryModel,
    quranMeta: QuranMeta
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth()
            .padding(start = 10.dp, bottom = 10.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                val intent = ReaderFactory.prepareLastVersesIntent(quranMeta, historyItem)
                if (intent != null) {
                    intent.setClass(context, ActivityReader::class.java)
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorResource(id = R.color.colorBGLastVersesChapterNo)),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = modifier
                    .height(70.dp)
                    .width(70.dp),
                backgroundColor = colorResource(id = R.color.colorBGLastVersesChapterNo),
                shape = RoundedCornerShape(12.dp),

                ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = historyItem.chapterNo.toString(),
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            ReadHistoryText(
                quranMeta = quranMeta,
                historyItem = historyItem
            )

        }
    }

}

@Composable
fun ReadHistoryText(
    modifier: Modifier = Modifier,
    quranMeta: QuranMeta,
    historyItem: ReadHistoryModel
) {
    val context = LocalContext.current
    val chapterName: String = quranMeta.getChapterName(context, historyItem.chapterNo, true) ?: ""
    var subTitle = stringResource(
        id = R.string.strLabelVersesWithColon,
        historyItem.fromVerseNo, historyItem.toVerseNo
    )
    if (QuranUtils.doesRangeDenoteSingle(historyItem.fromVerseNo, historyItem.toVerseNo)) {
        subTitle = stringResource(id = R.string.strLabelVerseNoWithColon, historyItem.fromVerseNo)
    }

    Column(
        modifier = modifier.padding(
            start = dimensionResource(id = R.dimen.dmnCommonSize2),
            end = dimensionResource(id = R.dimen.dmnCommonSize2)
        ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        //title
        Text(
            text = chapterName,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
        //subTitle
        Text(
            text = subTitle,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 3.dp)
        )
        //continueReading
        Text(
            text = stringResource(id = R.string.strLabelContinueReading),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Normal,
            color = colorResource(id = R.color.colorPrimary),
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.dmnCommonSize2))
        )
    }

}