package com.quranapp.android.views.homepage

import android.content.Context
import android.util.AttributeSet
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.adapters.ADPSituationVerse
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.SituationVerse
import com.quranapp.android.components.quran.VerseReference
import com.quranapp.android.databinding.LytHomepageTitledItemTitleBinding
import com.quranapp.android.utils.extensions.color

class SituationVersesLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HomepageCollectionLayoutBase(context, attrs, defStyleAttr) {
    override fun getHeaderTitle(): Int {
        return /*R.string.titleSituationVerses Verses for situation TODO*/ 0
    }

    override fun getHeaderIcon(): Int {
        return R.drawable.dr_icon_hash
    }

    override fun showViewAllBtn(): Boolean {
        return true
    }

    override fun setupHeader(context: Context, header: LytHomepageTitledItemTitleBinding) {
        header.titleIcon.setColorFilter(context.color(R.color.colorDanger))
    }

    override fun onViewAllClick(context: Context) {
//        context.startActivity(Intent(context, ActivitySituationVerses::class.java)) TODO
    }

    private fun refreshVerses(ctx: Context, verses: List<VerseReference>) {
        hideLoader()

        val featured = verses.subList(0, verses.size.coerceAtMost(10))
        resolveListView().adapter = ADPSituationVerse(ctx, Dimen.dp2px(ctx, 200f), featured)
    }

    fun refresh(quranMeta: QuranMeta) {
        showLoader()

        SituationVerse.prepareInstance(context, quranMeta) { duas ->
            refreshVerses(context, duas)
        }
    }
}