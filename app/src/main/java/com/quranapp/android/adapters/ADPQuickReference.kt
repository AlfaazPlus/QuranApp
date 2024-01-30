package com.quranapp.android.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.activities.ReaderPossessingActivity
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.views.reader.VerseView

class ADPQuickReference(private val mActivity: ReaderPossessingActivity) :
    RecyclerView.Adapter<ADPQuickReference.VHReader>() {
    private var mVerses: List<Verse>? = null

    init {
        setHasStableIds(true)
    }

    fun setVerses(verses: List<Verse>?) {
        mVerses = verses
    }

    override fun getItemCount(): Int {
        return mVerses!!.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): VHReader {
        return VHReader(VerseView(mActivity, parent, null, true))
    }

    override fun onBindViewHolder(holder: VHReader, position: Int) {
        holder.bind(mVerses!![position])
    }

    class VHReader(verseView: VerseView) : RecyclerView.ViewHolder(verseView) {
        private val mVerseView: VerseView?

        init {
            mVerseView = verseView
        }

        fun bind(verse: Verse?) {
            if (mVerseView == null) {
                return
            }
            mVerseView.verse = verse
        }
    }
}
