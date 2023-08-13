package com.quranapp.android.adapters.reference

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.databinding.LytQuranExclusiveVerseItemBinding
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.colorStateList
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.gesture.HoverPushEffect
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect

abstract class ADPExclusiveVerses(
    ctx: Context,
    private val itemWidth: Int,
    private val references: List<ExclusiveVerse>,
) : RecyclerView.Adapter<ADPExclusiveVerses.VHExclusiveVerse>() {
    private val txtSize = ctx.getDimenPx(R.dimen.dmnCommonSize2)
    private val txtSizeName = ctx.getDimenPx(R.dimen.dmnCommonSizeLarge)
    private val titleColor = ColorStateList.valueOf(ctx.color(R.color.white))
    private val infoColor = ColorStateList.valueOf(Color.parseColor("#D0D0D0"))

    override fun getItemCount(): Int {
        return references.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHExclusiveVerse {
        return VHExclusiveVerse(
            LytQuranExclusiveVerseItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHExclusiveVerse, position: Int) {
        holder.bind(references[position])
    }

    protected fun prepareTexts(title: String, subTitle: CharSequence?, inChapters: String?): CharSequence {
        val ssb = SpannableStringBuilder()
        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        val titleSS = SpannableString(title).apply {
            setSpan(
                TextAppearanceSpan("sans-serif", Typeface.BOLD, txtSizeName, titleColor, null),
                0,
                length,
                flag
            )
        }
        ssb.append(titleSS)

        if (subTitle != null) {
            val subTitleSS = SpannableString(subTitle).apply {
                setSpan(TextAppearanceSpan("sans-serif", Typeface.NORMAL, txtSize, infoColor, null), 0, length, flag)
                setSpan(LineHeightSpan2(20, false, true), 0, length, flag)
            }
            ssb.append("\n").append(subTitleSS)
        }

        if (inChapters != null) {
            val chaptersSS = SpannableString(inChapters).apply {
                setSpan(
                    TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, txtSize, infoColor, null),
                    0,
                    length,
                    flag
                )
            }
            ssb.append("\n").append(chaptersSS)
        }

        return ssb
    }

    abstract fun onBind(binding: LytQuranExclusiveVerseItemBinding, verse: ExclusiveVerse)

    inner class VHExclusiveVerse(private val binding: LytQuranExclusiveVerseItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.apply {
                setOnTouchListener(HoverPushOpacityEffect(HoverPushEffect.Pressure.LOW))
                layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        fun bind(verse: ExclusiveVerse) {
            onBind(binding, verse)
        }
    }
}