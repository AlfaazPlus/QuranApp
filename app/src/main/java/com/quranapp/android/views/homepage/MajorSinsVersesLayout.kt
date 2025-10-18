package com.quranapp.android.views.homepage2

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.ViewGroup
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityMajorSins
import com.quranapp.android.adapters.reference.ADPMajorSins
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.QuranMajorSins
import com.quranapp.android.components.quran.QuranMeta

class MajorSinsVersesLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HomepageCollectionLayoutBase(context, attrs, defStyleAttr) {
    override fun getHeaderTitle(): Int {
        return R.string.strTitleMajorSins
    }

    override fun getHeaderIcon(): Int {
        return R.drawable.icon_major_sins
    }

    override fun onViewAllClick() {
        context.startActivity(Intent(context, ActivityMajorSins::class.java))
    }

    private fun refreshVerses(ctx: Context, verses: List<ExclusiveVerse>) {
        hideLoader()

        val featured = verses.subList(0, verses.size.coerceAtMost(5))
        resolveListView().apply {
            adapter = ADPMajorSins(
                ctx,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Dimen.dp2px(ctx, 260f),
                featured
            )
        }
    }

    override fun refresh(quranMeta: QuranMeta) {
        showLoader()

        QuranMajorSins.prepareInstance(context, quranMeta) { references ->
            refreshVerses(context, references)
        }
    }
}