package com.quranapp.android.components.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.quran.RevelationType
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_chevron_right
import com.quranapp.android.generated.resources.dr_icon_info
import com.quranapp.android.generated.resources.dr_madina_old
import com.quranapp.android.generated.resources.dr_makkah_old
import com.quranapp.android.generated.resources.strChapInfoSeeMore
import com.quranapp.android.generated.resources.strLabelOrder
import com.quranapp.android.generated.resources.strLabelSurah
import com.quranapp.android.generated.resources.strTitleChapInfoRukus
import com.quranapp.android.generated.resources.strTitleMadani
import com.quranapp.android.generated.resources.strTitleMakki
import com.quranapp.android.generated.resources.verses
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.miscellenous.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChapterInfoCard(chapterNo: Int) {
    val context = LocalContext.current
    val appLocale = LocalAppLocale.current

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    var _swl by remember { mutableStateOf<SurahWithLocalizations?>(null) }

    LaunchedEffect(chapterNo) {
        _swl = withContext(Dispatchers.IO) {
            repository.getSurahWithLocalizations(chapterNo)
        }
    }

    val swl = _swl ?: return

    var expanded by remember(chapterNo) { mutableStateOf(false) }

    val isMeccan = swl.surah.revelationType == RevelationType.meccan

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_info),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(Res.string.strLabelSurah, swl.getCurrentName()),
                        style = typography.titleSmall,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) -90f else 0f),
                    tint = colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .clickable {
                            val intent =
                                AppDestinations.intent(context, AppDestinations.CHAP_INFO).apply {
                                    putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
                                }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colorScheme.primary,
                        ) {
                            Text(
                                text = stringResource(
                                    if (isMeccan) Res.string.strTitleMakki else Res.string.strTitleMadani,
                                ),
                                style = typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChapterInfoStatChip(
                                text = stringResource(Res.string.verses) + ": " +
                                        appLocale.format(
                                            "%d",
                                            swl.surah.ayahCount
                                        ),
                            )
                            ChapterInfoStatChip(
                                text = stringResource(Res.string.strTitleChapInfoRukus) + ": " +
                                        appLocale.format(
                                            "%d",
                                            swl.surah.rukusCount
                                        ),
                            )
                            ChapterInfoStatChip(
                                text = stringResource(Res.string.strLabelOrder) + ": " +
                                        appLocale.format(
                                            "%d",
                                            swl.surah.revelationOrder
                                        ),
                            )
                        }

                        Text(
                            text = stringResource(Res.string.strChapInfoSeeMore),
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Image(
                        painter = painterResource(
                            if (isMeccan) Res.drawable.dr_makkah_old else Res.drawable.dr_madina_old,
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(72.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterInfoStatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = colorScheme.background
    ) {
        Text(
            text = text,
            style = typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(6.dp),
        )
    }
}
