package com.quranapp.android.views.homepage

import android.content.Context
import android.util.AttributeSet
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.adapters.ADPReadHistory
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.utils.sharedPrefs.SPLastRead
import com.quranapp.android.views.homepage2.HomepageCollectionLayoutBase

class LastReadLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HomepageCollectionLayoutBase(context, attrs, defStyleAttr) {

    private var adapter: ADPReadHistory? = null

    override fun refresh(quranMeta: QuranMeta) {
        val lastReadEntries = SPLastRead.getAllLastRead(context)
        if (lastReadEntries.isEmpty()) {
            visibility = GONE
            return
        }

        visibility = VISIBLE
        val models = lastReadEntries.sortedByDescending { it.timestamp }.map { entry ->
            ReadHistoryModel(
                entry.chapterNo.toLong(),
                0,
                0,
                -1,
                entry.chapterNo,
                entry.verseNo,
                entry.verseNo,
                ""
            )
        }

        if (adapter == null) {
            adapter = ADPReadHistory(context, quranMeta, models, Dimen.dp2px(context, 280f))
            resolveListView().adapter = adapter
        } else {
            adapter!!.updateModels(models)
        }
    }

    override fun getHeaderTitle(): Int {
        return R.string.strTitleLastRead
    }

    override fun getHeaderIcon(): Int {
        return R.drawable.dr_icon_read_quran
    }

    override fun showViewAllBtn(): Boolean {
        return false
    }

    fun destroy() {
        adapter = null
    }
}
