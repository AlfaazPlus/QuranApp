package com.quranapp.android.adapters.quranIndex

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.frags.readerindex.BaseFragReaderIndex
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updateMargins
import com.quranapp.android.utils.reader.factory.ReaderFactory.startChapter
import com.quranapp.android.widgets.chapterCard.ChapterCard

class ADPFavChaptersList(
    private val fragment: BaseFragReaderIndex,
    private val chapterNos: ArrayList<Int>
) : RecyclerView.Adapter<ADPFavChaptersList.VHChapter>() {
    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = chapterNos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChapter {
        val chapterCard = ChapterCard(parent.context).apply {
            setBackgroundResource(R.drawable.dr_bg_chapter_card)
            elevation = parent.context.dp2px(2f).toFloat()
        }

        val params = chapterCard.layoutParams

        if (params is MarginLayoutParams) {
            params.updateMargins(parent.context.dp2px(5f))
        }

        return VHChapter(chapterCard)
    }

    override fun onBindViewHolder(holder: VHChapter, position: Int) {
        holder.bind(chapterNos[position])
    }

    inner class VHChapter(private val chapterCard: ChapterCard) : RecyclerView.ViewHolder(chapterCard) {
        fun bind(chapterNo: Int) {
            chapterCard.let {
                it.setChapterNumber(chapterNo)
                it.setName(
                    fragment.quranMeta.getChapterName(itemView.context, chapterNo),
                    fragment.quranMeta.getChapterNameTranslation(chapterNo)
                )
                it.setOnClickListener { v -> startChapter(v.context, chapterNo) }
            }
        }
    }
}