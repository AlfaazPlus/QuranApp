package com.quranapp.android.ui.components.homepage.quranEtiquette


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QuranEtiquetteItem(
    modifier: Modifier = Modifier,
    referenceItem: ExclusiveVerse,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, color = colorResource(id = R.color.colorDividerVariable)),
        onClick = { onClick() },
        backgroundColor = colorResource(id = R.color.colorBGCardVariable)
    ) {
        Text(
            text = referenceItem.name,
            fontSize = dimensionResource(id = R.dimen.dmnCommonSizeLarge).value.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            color = colorResource(id = R.color.colorText),
            modifier = Modifier.padding(15.dp, 12.dp)
        )
    }

}
