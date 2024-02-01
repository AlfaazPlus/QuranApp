package com.quranapp.android.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.adapters.ADPQuranPages.VHQuranPage
import com.quranapp.android.components.quran.QuranMeta.ChapterMeta
import com.quranapp.android.components.reader.QuranPageModel
import com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType
import com.quranapp.android.utils.extensions.updateMargins
import com.quranapp.android.views.reader.ChapterInfoCardView
import com.quranapp.android.views.reader.QuranPageView

class ADPQuranPages(private val mActivity: ActivityReader, private val mChapterInfoMeta: ChapterMeta?, private val mModels: ArrayList<QuranPageModel>)
    : RecyclerView.Adapter<ADPQuranPages.VHQuranPage>() {
    init {
        if (mChapterInfoMeta != null) {
            mModels.add(0, QuranPageModel().setViewType(RecyclerItemViewType.CHAPTER_INFO))
        }
        mModels.add(mModels.size, QuranPageModel().setViewType(RecyclerItemViewType.READER_FOOTER))

        for (i in mModels.indices) {
            val model = mModels[i]
            if (model.viewType == RecyclerItemViewType.READER_PAGE) {
                for (section in model.sections) {
                    section.parentIndexInAdapter = i
                }
            }
        }

        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return mModels.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun getViewType(position: Int): Int {
        return mModels[position].viewType
    }

    fun highlightVerseOnScroll(position: Int, chapterNo: Int, verseNo: Int) {
        val pageModel = getPageModel(position)
        pageModel.scrollHighlightPendingChapterNo = chapterNo
        pageModel.scrollHighlightPendingVerseNo = verseNo
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): VHQuranPage {
        val viewType = getViewType(position)
        val view: View
        when (viewType) {
            RecyclerItemViewType.CHAPTER_INFO -> {
                view = ChapterInfoCardView(mActivity)
            }
            RecyclerItemViewType.READER_PAGE -> {
                val quranPageView = QuranPageView(mActivity)
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.updateMargins(Dimen.dp2px(parent.context, 3f))
                quranPageView.layoutParams = params
                view = quranPageView
            }
            RecyclerItemViewType.READER_FOOTER -> {
                val footer = mActivity.mNavigator.readerFooter
                footer.clearParent()
                view = footer
            }
            else -> {
                view = View(parent.context)
            }
        }

        var params = view.layoutParams
        if (params == null) {
            params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        view.layoutParams = params

        return VHQuranPage(view)
    }

    override fun onBindViewHolder(holder: VHQuranPage, position: Int) {
        holder.bind(mModels[position])
    }

    fun getPageModel(position: Int): QuranPageModel {
        return mModels[position]
    }

    inner class VHQuranPage(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(pageModel: QuranPageModel?) {
            val position = bindingAdapterPosition
            val viewType = getViewType(position)

            if (viewType == RecyclerItemViewType.CHAPTER_INFO) {
                (itemView as ChapterInfoCardView).setInfo(mChapterInfoMeta)
            } else if (viewType == RecyclerItemViewType.READER_PAGE) {
                (itemView as QuranPageView).setPageModel(pageModel)
            }
        }
    }
}
