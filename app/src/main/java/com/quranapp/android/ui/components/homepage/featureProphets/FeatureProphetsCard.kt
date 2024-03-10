package com.quranapp.android.ui.components.homepage.featureProphets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.utils.reader.factory.ReaderFactory
import java.text.MessageFormat

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FeatureProphetsCard(
    modifier: Modifier = Modifier,
    prophet: QuranProphet.Prophet
) {
    val context = LocalContext.current
    val name = MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
    val nameEng = "English : " + prophet.nameEn

    Card(
        modifier = Modifier
            .width(310.dp)
            .padding(start = 10.dp, bottom = 20.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        onClick = {
            val title = context.getString(
                R.string.strMsgReferenceInQuran,
                MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
            )
            val desc =
                context.getString(R.string.strMsgReferenceFoundPlaces, title, prophet.verses.size)
            val intent = ReaderFactory.prepareReferenceVerseIntent(
                true,
                title,
                desc,
                arrayOf(),
                prophet.chapters,
                prophet.verses
            )

            intent.setClass(context, ActivityReference::class.java)
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorResource(id = R.color.colorBGLastVersesChapterNo)),
        elevation = 3.dp,
        backgroundColor = colorResource(id = R.color.colorBGHomePageItem)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = prophet.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .height(80.dp)
                    .width(80.dp)
            )
            FeatureProphetsText(
                title = name,
                subTitle = nameEng,
                inChapters = prophet.inChapters
            )
        }

    }
}

@Composable
fun FeatureProphetsText(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String,
    inChapters: String?
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, top = 10.dp, end = 15.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        //title
        Text(
            text = title,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(id = R.dimen.dmnCommonSize2).value.sp,
            color = colorResource(id = R.color.colorText)
        )
        //subTitle
        Text(
            text = subTitle,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = dimensionResource(id = R.dimen.dmnCommonSize2).value.sp,
            color = colorResource(id = R.color.colorText),
            lineHeight = 20.sp
        )

        //inChapters
        if (inChapters != null) {
            Text(
                text = inChapters,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Normal,
                fontSize = dimensionResource(id = R.dimen.dmnCommonSize2).value.sp,
                color = colorResource(id = R.color.colorText2),
                modifier = Modifier.padding(top = 10.dp)
            )
        }

    }
}