package com.quranapp.android.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.components.FeaturedQuranModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.components.readHistory.mapToEntity
import com.quranapp.android.db.entities.ReadHistory
import com.quranapp.android.db.entities.mapToUiModel
import com.quranapp.android.db.readHistory.ReadHistoryDBHolder
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.univ.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class FeaturedReadingViewModel : ViewModel() {
    val models = mutableListOf <FeaturedQuranModel>()
    var quranMeta = MutableStateFlow(QuranMeta())
        private set
    var isLoading by mutableStateOf(false)

    fun init(context: Context) {
        isLoading = true
        QuranMeta.prepareInstance(context,
            object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    quranMeta.update { r }
                    getFeaturedQuranModels(context)
                }
            }
        )
    }

    fun getFeaturedQuranModels(context: Context){
        val itemsArray = context.resources.obtainTypedArray(R.array.arrFeaturedQuranItems)
        val chapNameFormat = context.getString(R.string.strLabelSurah)
        val verseNoFormat = context.getString(R.string.strLabelVerseNo)
        val versesFormat = context.getString(R.string.strLabelVerses)
        val miniInfoFormat = context.getString(R.string.strLabelVerseWithChapNameWithBar)
        val miniInfoChapFormat = context.getString(R.string.strLabelFeatureQuranMiniInfo)

        for (i in 0 until itemsArray.length()) {
            val model = FeaturedQuranModel()
            val splits = itemsArray.getString(i)!!.split(":")

            val chapterNo = splits[0].toInt()
            val verses = IntArray(2)

            if (splits.size >= 2) {
                val versesSplits = splits[1].split("[–-]".toRegex())
                val locale = Locale.getDefault()

                verses[0] = versesSplits[0].toInt()

                if (versesSplits.size >= 2) {
                    verses[1] = versesSplits[1].toInt()
                    model.name = String.format(locale, chapNameFormat, quranMeta.value.getChapterName(context, chapterNo))
                    model.miniInfo = String.format(locale, versesFormat, verses[0], verses[1])
                } else {
                    verses[1] = verses[0]

                    val chapterName = quranMeta.value.getChapterName(context, chapterNo)
                    if (chapterNo == 2 && verses[0] == 255) {
                        model.name = context.getString(R.string.strAyatulKursi)
                        model.miniInfo = String.format(locale, miniInfoFormat, chapterName, 255)
                    } else {
                        model.name = String.format(locale, chapNameFormat, chapterName)
                        model.miniInfo = String.format(locale, verseNoFormat, verses[0])
                    }
                }
            } else {
                verses[0] = 1
                verses[1] = quranMeta.value.getChapterVerseCount(chapterNo)

                model.name = String.format(chapNameFormat, quranMeta.value.getChapterName(context, chapterNo))
                model.miniInfo = String.format(miniInfoChapFormat, chapterNo, 1, verses[1])
            }

            model.chapterNo = chapterNo
            model.verseRange = Pair(verses[0], verses[1])
            models.add(model)
        }

        itemsArray.recycle()

        isLoading = false
    }


}