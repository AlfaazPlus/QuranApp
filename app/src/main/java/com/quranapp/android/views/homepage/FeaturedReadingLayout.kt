package com.quranapp.android.views.homepage2

import android.content.Context
import android.util.AttributeSet
import com.quranapp.android.R
import com.quranapp.android.adapters.ADPFeaturedQuran
import com.quranapp.android.components.FeaturedQuranModel
import com.quranapp.android.components.quran.QuranMeta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FeaturedReadingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HomepageCollectionLayoutBase(context, attrs, defStyleAttr) {
    private var firstTime = true

    override fun getHeaderTitle(): Int {
        return R.string.strTitleFeaturedQuran;
    }

    override fun getHeaderIcon(): Int {
        return R.drawable.dr_icon_feature;
    }

    override fun showViewAllBtn(): Boolean {
        return false
    }

    override fun refresh(quranMeta: QuranMeta) {
        if (firstTime) {
            showLoader()
            firstTime = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            val models = getFeaturedQuranModels(quranMeta)

            withContext(Dispatchers.Main) {
                hideLoader()
                resolveListView().adapter = ADPFeaturedQuran(quranMeta, models)
            }
        }
    }

    private fun getFeaturedQuranModels(quranMeta: QuranMeta): MutableList<FeaturedQuranModel> {
        val models = ArrayList<FeaturedQuranModel>()
        val itemsArray = resources.obtainTypedArray(R.array.arrFeaturedQuranItems)
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
                    model.name = String.format(locale, chapNameFormat, quranMeta.getChapterName(context, chapterNo))
                    model.miniInfo = String.format(locale, versesFormat, verses[0], verses[1])
                } else {
                    verses[1] = verses[0]

                    val chapterName = quranMeta.getChapterName(context, chapterNo)
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
                verses[1] = quranMeta.getChapterVerseCount(chapterNo)

                model.name = String.format(chapNameFormat, quranMeta.getChapterName(context, chapterNo))
                model.miniInfo = String.format(miniInfoChapFormat, chapterNo, 1, verses[1])
            }

            model.chapterNo = chapterNo
            model.verseRange = Pair(verses[0], verses[1])
            models.add(model)
        }

        itemsArray.recycle()
        return models
    }
}