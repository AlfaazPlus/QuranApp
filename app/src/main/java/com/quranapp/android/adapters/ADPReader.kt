package com.quranapp.android.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.components.quran.QuranMeta.ChapterMeta
import com.quranapp.android.components.reader.ReaderRecyclerItemModel
import com.quranapp.android.components.utility.CardMessageParams
import com.quranapp.android.databinding.LytReaderIsVotdBinding
import com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType
import com.quranapp.android.views.CardMessage
import com.quranapp.android.views.reader.BismillahView
import com.quranapp.android.views.reader.ChapterInfoCardView
import com.quranapp.android.views.reader.ChapterTitleView
import com.quranapp.android.views.reader.VerseView

class ADPReader(private val mActivity: ActivityReader, private val mChapterInfoMeta: ChapterMeta?, private val mModels: ArrayList<ReaderRecyclerItemModel>) :
    RecyclerView.Adapter<ADPReader.VHReader>() {
    init {
        if (mChapterInfoMeta != null) {
            mModels.add(0, ReaderRecyclerItemModel().setViewType(RecyclerItemViewType.CHAPTER_INFO))
        }
        mModels.add(mModels.size, ReaderRecyclerItemModel().setViewType(RecyclerItemViewType.READER_FOOTER))

        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return mModels.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return getViewType(position)
    }

    private fun getViewType(position: Int): Int {
        return mModels[position].viewType
    }

    fun getItem(position: Int): ReaderRecyclerItemModel {
        return mModels[position]
    }

    fun highlightVerseOnScroll(position: Int) {
        getItem(position).isScrollHighlightPending = true
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHReader {
        val view: View = when (viewType) {
            RecyclerItemViewType.IS_VOTD -> {
                makeIsVotdView(mActivity, parent)
            }
            RecyclerItemViewType.NO_TRANSL_SELECTED -> {
                prepareNoTranslMessageView(mActivity)
            }
            RecyclerItemViewType.CHAPTER_INFO -> {
                ChapterInfoCardView(mActivity)
            }
            RecyclerItemViewType.BISMILLAH -> {
                BismillahView(mActivity)
            }
            RecyclerItemViewType.VERSE -> {
                VerseView(mActivity, parent, null, false)
            }
            RecyclerItemViewType.CHAPTER_TITLE -> {
                ChapterTitleView(mActivity)
            }
            RecyclerItemViewType.READER_FOOTER -> {
                val footer = mActivity.mNavigator.readerFooter
                footer.clearParent()
                footer
            }
            else -> {
                View(mActivity)
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

        return VHReader(view)
    }

    private fun makeIsVotdView(activity: ActivityReader, parent: ViewGroup): View {
        val binding = LytReaderIsVotdBinding.inflate(activity.layoutInflater, parent, false)
        return binding.root
    }

    private fun prepareNoTranslMessageView(activity: ActivityReader): View {
        val msgView = CardMessage(activity)
        msgView.apply {
            setMessage(activity.str(R.string.strMsgTranslNoneSelected))
            elevation = activity.dp2px(4f).toFloat()
            setMessageStyle(CardMessageParams.STYLE_WARNING)
            setActionText(activity.str(R.string.strTitleSettings)) {
                activity.mBinding.readerHeader.openReaderSetting(ActivitySettings.SETTINGS_TRANSLATION)
            }
        }
        return msgView
    }

    override fun onBindViewHolder(holder: VHReader, position: Int) {
        val model = mModels[position]
        holder.bind(model)
    }

    inner class VHReader(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(model: ReaderRecyclerItemModel) {
            val position = bindingAdapterPosition
            when (getViewType(position)) {
                RecyclerItemViewType.CHAPTER_INFO -> (itemView as ChapterInfoCardView).setInfo(mChapterInfoMeta)
                RecyclerItemViewType.VERSE -> setupVerseView(model)
                RecyclerItemViewType.CHAPTER_TITLE -> setupTitleView(model)
            }
        }

        private fun setupVerseView(model: ReaderRecyclerItemModel) {
            if (itemView !is VerseView) return

            val verse = model.verse

            val verseView = itemView as VerseView
            verseView.verse = verse

            if (model.isScrollHighlightPending) {
                verseView.highlightOnScroll()
                model.isScrollHighlightPending = false
            }

            if (mActivity.mPlayer != null) {
                verseView.onRecite(mActivity.mPlayer.isReciting(verse.chapterNo, verse.verseNo))
            }
        }

        private fun setupTitleView(model: ReaderRecyclerItemModel) {
            if (itemView !is ChapterTitleView) return

            val chapterTitleView = itemView as ChapterTitleView
            chapterTitleView.setChapterNumber(model.chapterNo)
        }
    }
}
