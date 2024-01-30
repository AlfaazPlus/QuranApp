package com.quranapp.android.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Dimension
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.databinding.LytBookmarkItemBinding
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.extensions.updateMargins
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareLastVersesIntent
import java.util.Locale

class ADPReadHistory(
    ctx: Context, private val mQuranMeta: QuranMeta, private var mHistories: List<ReadHistoryModel>,
    @field:Dimension private val mItemWidth: Int
) : RecyclerView.Adapter<ADPReadHistory.VHReadHistory>() {
    private val mTxtSize: Int
    private val mTxtSize2: Int
    private val mColorPrimary: ColorStateList?
    private val mVerseNoFormat: String
    private val mVersesFormat: String
    private val mTxtContinueReading: String

    init {
        mTxtSize = ctx.getDimenPx(R.dimen.dmnCommonSize2)
        mTxtSize2 = ctx.getDimenPx(R.dimen.dmnCommonSize2_5)
        mColorPrimary = ContextCompat.getColorStateList(ctx, R.color.colorPrimary)

        mVerseNoFormat = ctx.getString(R.string.strLabelVerseNoWithColon)
        mVersesFormat = ctx.getString(R.string.strLabelVersesWithColon)
        mTxtContinueReading = ctx.getString(R.string.strLabelContinueReading)
    }

    fun updateModels(models: List<ReadHistoryModel>) {
        mHistories = models
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mHistories.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHReadHistory {
        val binding = LytBookmarkItemBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return VHReadHistory(binding)
    }

    override fun onBindViewHolder(holder: VHReadHistory, position: Int) {
        holder.bind(mHistories[position])
    }

    private fun prepareTexts(title: String, subTitle: CharSequence, continueReading: String): CharSequence {
        val titleSS = SpannableString(title)
        val titleTASpan = TextAppearanceSpan("sans-serif", Typeface.BOLD, mTxtSize, null, null)
        titleSS.setSpan(titleTASpan, 0, titleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val subTitleSS = SpannableString(subTitle)
        val subTitleTASpan = TextAppearanceSpan("sans-serif", Typeface.NORMAL, mTxtSize, null, null)
        subTitleSS.setSpan(subTitleTASpan, 0, subTitleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        subTitleSS.setSpan(LineHeightSpan2(20, false, true), 0,
            subTitleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val continueReadingSS = SpannableString(continueReading)
        val continueReadingTASpan = TextAppearanceSpan("sans-serif-medium", Typeface.NORMAL,
            mTxtSize2, mColorPrimary, null)
        continueReadingSS.setSpan(continueReadingTASpan, 0, continueReadingSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", continueReadingSS)
    }

    inner class VHReadHistory(private val mBinding: LytBookmarkItemBinding) : RecyclerView.ViewHolder(mBinding.root) {
        init {
            val root: View = mBinding.root
            root.elevation = Dimen.dp2px(root.context, 4f).toFloat()

            var p = root.layoutParams
            if (p != null) {
                p.width = mItemWidth
            } else {
                p = RecyclerView.LayoutParams(mItemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                (p as ViewGroup.MarginLayoutParams).updateMargins(Dimen.dp2px(mBinding.root.context, 3f))
            }
            root.layoutParams = p

            if (mItemWidth > 0) {
                root.setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered)
            } else {
                root.setBackgroundResource(R.drawable.dr_bg_chapter_card)
            }
        }

        fun bind(history: ReadHistoryModel) {
            mBinding.chapterNo.visibility = View.VISIBLE
            mBinding.menu.visibility = View.GONE
            mBinding.check.visibility = View.GONE

            mBinding.chapterNo.text = String.format(Locale.getDefault(), "%d", history.chapterNo)

            val chapterName = mQuranMeta.getChapterName(itemView.context, history.chapterNo, true)
            val txt = prepareTexts(chapterName, prepareSubtitleTitle(history.fromVerseNo, history.toVerseNo),
                mTxtContinueReading)
            mBinding.text.text = txt

            setupActions(history)
        }

        private fun setupActions(history: ReadHistoryModel) {
            mBinding.root.setOnClickListener { v ->
                val intent = prepareLastVersesIntent(mQuranMeta, history)

                if (intent != null) {
                    intent.setClass(itemView.context, ActivityReader::class.java)
                    itemView.context.startActivity(intent)
                }
            }
        }

        fun prepareSubtitleTitle(fromVerse: Int, toVerse: Int): CharSequence {
            return if (QuranUtils.doesRangeDenoteSingle(fromVerse, toVerse)) {
                String.format(mVerseNoFormat, fromVerse)
            } else {
                String.format(mVersesFormat, fromVerse, toVerse)
            }
        }
    }
}
