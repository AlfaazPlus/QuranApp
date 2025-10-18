package com.quranapp.android.adapters.reference

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.databinding.LytMajorSinItemBinding
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.gesture.HoverPushEffect
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect
import com.quranapp.android.utils.reader.factory.ReaderFactory

class ADPMajorSins(
    ctx: Context,
    private val itemHeight: Int,
    private val itemWidth: Int,
    private val references: List<ExclusiveVerse>
) : RecyclerView.Adapter<ADPMajorSins.VHMajorSinVerse>() {
    private val txtSize = ctx.getDimenPx(R.dimen.dmnCommonSize2)
    private val txtColor2 = ContextCompat.getColorStateList(ctx, R.color.colorText2)

    override fun getItemCount(): Int {
        return references.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHMajorSinVerse {
        return VHMajorSinVerse(
            LytMajorSinItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHMajorSinVerse, position: Int) {
        holder.bind(references[position])
    }

    inner class VHMajorSinVerse(private val binding: LytMajorSinItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.apply {
                setOnTouchListener(HoverPushOpacityEffect(HoverPushEffect.Pressure.LOW))
            }
        }

        fun bind(verse: ExclusiveVerse) {
            binding.text.text = verse.title

            if (itemHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                (binding.text.layoutParams as LinearLayout.LayoutParams).weight = 1f
            }

            val context = binding.root.context
            val count = verse.verses.size

            binding.verseRef.text = prepareReferenceTexts(
                if (count > 1) context.getString(R.string.places, count)
                else context.getString(R.string.place, count),
                verse.inChapters
            )

            binding.root.apply {
                layoutParams =
                    ViewGroup.LayoutParams(itemWidth, itemHeight)

                setOnClickListener {
                    ReaderFactory.startReferenceVerse(
                        context,
                        true,
                        verse.title,
                        verse.description,
                        arrayOf(),
                        verse.chapters,
                        verse.versesRaw
                    )
                }
            }
        }


        protected fun prepareReferenceTexts(
            subTitle: CharSequence?,
            inChapters: String?
        ): CharSequence {
            val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

            val chaptersSS = SpannableString(inChapters).apply {
                setSpan(
                    TextAppearanceSpan(
                        "sans-serif-light",
                        Typeface.NORMAL,
                        txtSize,
                        txtColor2,
                        null
                    ),
                    0,
                    length,
                    flag
                )
            }

            return chaptersSS
        }
    }
}