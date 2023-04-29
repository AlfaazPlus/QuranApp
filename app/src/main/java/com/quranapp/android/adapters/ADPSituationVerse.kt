package com.quranapp.android.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.components.quran.VerseReference
import com.quranapp.android.databinding.LytQuranTopicItemBinding
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.reader.factory.ReaderFactory.startReferenceVerse

class ADPSituationVerse(
    ctx: Context,
    private val itemWidth: Int,
    private val duas: List<VerseReference>
) : RecyclerView.Adapter<ADPSituationVerse.VHSituationVerse>() {
    private val txtSize = ctx.getDimenPx(R.dimen.dmnCommonSize2)
    private val infoColor = ColorStateList.valueOf(ctx.color(R.color.colorText2))

    override fun getItemCount(): Int {
        return duas.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSituationVerse {
        return VHSituationVerse(LytQuranTopicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VHSituationVerse, position: Int) {
        holder.bind(duas[position])
    }

    private fun prepareTexts(title: String, subTitle: CharSequence, inChapters: String): CharSequence {
        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        val titleSS = SpannableString(title).apply {
            setSpan(
                TextAppearanceSpan("sans-serif", Typeface.BOLD, txtSize, null, null),
                0,
                length,
                flag
            )
        }

        val subTitleSS = SpannableString(subTitle).apply {
            setSpan(TextAppearanceSpan("sans-serif", Typeface.NORMAL, txtSize, null, null), 0, length, flag)
            setSpan(LineHeightSpan2(20, false, true), 0, length, flag)
        }

        val chaptersSS = SpannableString(inChapters).apply {
            setSpan(TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, txtSize, infoColor, null), 0, length, flag)
        }

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", chaptersSS)
    }

    inner class VHSituationVerse(private val binding: LytQuranTopicItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.apply {
                layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                elevation = Dimen.dp2px(context, 4f).toFloat()
                layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

                if (itemWidth > 0) {
                    minimumHeight = Dimen.dp2px(context, 110f)
                    setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered)
                } else {
                    setBackgroundResource(R.drawable.dr_bg_chapter_card)
                }
            }
        }

        fun bind(reference: VerseReference) {
            val ctx = itemView.context
            val count = reference.verses.size
            val countText = count.toString() + if (count == 1) " place" else " places"
            binding.text.text = prepareTexts(reference.name, countText, reference.inChapters)

            binding.root.setOnClickListener {
                var title = "\"${reference.name}\""
                val desc = ctx.getString(R.string.strMsgReferenceFoundPlaces, title, reference.verses.size)
                title = ctx.getString(R.string.strMsgReferenceInQuran, title)
                startReferenceVerse(ctx, true, title, desc, arrayOf(), reference.chapters, reference.versesRaw)
            }
        }
    }
}