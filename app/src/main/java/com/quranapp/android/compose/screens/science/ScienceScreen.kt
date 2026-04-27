package com.quranapp.android.compose.screens.science

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityQuranScienceContent
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeJsonObject
import com.quranapp.android.api.safeString
import com.quranapp.android.api.toStringMap
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.theme.alpha
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private fun getQuranScienceDrawableRes(id: String): Int {
    return when (id) {
        "astronomy" -> R.drawable.ic_science_astronomy
        "physics" -> R.drawable.ic_science_physics
        "geography" -> R.drawable.ic_science_geography
        "geology" -> R.drawable.ic_science_geology
        "oceanography" -> R.drawable.ic_science_oceanography
        "biology" -> R.drawable.ic_science_biology
        "botany" -> R.drawable.ic_science_botany
        "zoology" -> R.drawable.ic_science_zoology
        "medicine" -> R.drawable.ic_science_medicine
        "physiology" -> R.drawable.ic_science_physiology
        "embryology" -> R.drawable.ic_science_embryology
        "general_science" -> R.drawable.ic_science_general
        else -> R.drawable.ic_science_physics
    }
}

fun loadScienceItems(context: android.content.Context): List<QuranScienceItem> {
    val items = mutableListOf<QuranScienceItem>()

    context.assets.open("science/index.json").use { inputStream ->
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = Json.parseToJsonElement(json).jsonArray

        jsonArray.forEach {
            val item = it.jsonObject

            items.add(
                QuranScienceItem(
                    item.safeString("title", ""),
                    item.safeInt("referencesCount", 0),
                    item.safeString("path", ""),
                    getQuranScienceDrawableRes(item.safeString("id", "")),
                    item.safeJsonObject("translations")?.toStringMap() ?: mapOf()
                )
            )
        }
    }

    return items
}


@Composable
fun ScienceScreen() {
    val context = LocalContext.current
    var infoDialogShown by remember { mutableStateOf(false) }

    val scienceItems = remember {
        loadScienceItems(context)
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.quran_and_science),
                actions = {
                    IconButton(
                        painter = painterResource(R.drawable.dr_icon_info)
                    ) {
                        infoDialogShown = true
                    }
                }
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            val columns = when {
                maxWidth >= 900.dp -> GridCells.Fixed(3)
                else -> GridCells.Fixed(2)
            }
            LazyVerticalGrid(
                columns = columns,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(scienceItems, key = { it.path }) { item ->
                    ItemCard(item)
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
            stringResource(R.string.about_quran_science_msg)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCard(item: QuranScienceItem) {
    val context = LocalContext.current

    Card(
        onClick = {
            context.startActivity(
                Intent(context, ActivityQuranScienceContent::class.java).apply {
                    putExtra("item", item)
                }
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.alpha(0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFABABAB)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(74.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.getTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.colorText),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.strLabelScienceReferences, item.referencesCount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = colorResource(R.color.colorText2),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}