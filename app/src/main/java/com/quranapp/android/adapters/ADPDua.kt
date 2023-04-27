package com.quranapp.android.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.components.quran.dua.Dua
import com.quranapp.android.databinding.LytQuranDuaItemBinding
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.gesture.HoverPushEffect
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect
import com.quranapp.android.utils.reader.factory.ReaderFactory

class ADPDua(
    ctx: Context,
    private val itemWidth: Int,
    private val duas: List<Dua>
) : RecyclerView.Adapter<ADPDua.VHDua>() {
    private val txtSize = ctx.getDimenPx(R.dimen.dmnCommonSize2)
    private val txtSizeName = ctx.getDimenPx(R.dimen.dmnCommonSizeLarge)
    private val txtColor2 = ContextCompat.getColorStateList(ctx, R.color.colorText2)

    override fun getItemCount(): Int {
        return duas.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHDua {
        return VHDua(LytQuranDuaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VHDua, position: Int) {
        holder.bind(duas[position])
    }

    private fun prepareTexts(title: String, subTitle: CharSequence, inChapters: String): CharSequence {
        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        val titleSS = SpannableString(title).apply {
            setSpan(
                TextAppearanceSpan("sans-serif", Typeface.BOLD, txtSizeName, null, null),
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
            setSpan(TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, txtSize, txtColor2, null), 0, length, flag)
        }

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", chaptersSS)
    }

    inner class VHDua(private val binding: LytQuranDuaItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.apply {
                setOnTouchListener(HoverPushOpacityEffect(HoverPushEffect.Pressure.LOW))
                layoutParams = ViewGroup.LayoutParams(itemWidth, itemView.context.dp2px(150F))
            }
        }

        fun bind(dua: Dua) {
            val topicName = dua.name
            val count = dua.verses.size

            binding.text.text = prepareTexts(
                topicName,
                if (count > 1) itemView.context.getString(R.string.places, count)
                else itemView.context.getString(R.string.place, count),
                dua.inChapters
            )

            binding.root.setOnClickListener { v: View ->
                val ctx = v.context

                val excluded = dua.id in arrayOf(1, 8)

                val nameTitle = if (!excluded) ctx.getString(R.string.strMsgDuaFor, dua.name)
                else ctx.getString(R.string.strMsgReferenceInQuran, "\"" + dua.name + "\"")

                val description = ctx.getString(
                    R.string.strMsgReferenceFoundPlaces,
                    if (excluded) nameTitle else "\"" + nameTitle + "\"",
                    dua.verses.size
                )

                ReaderFactory.startReferenceVerse(
                    ctx,
                    true,
                    nameTitle,
                    description,
                    arrayOf(),
                    dua.chapters,
                    dua.versesRaw
                )
            }
        }
    }
}