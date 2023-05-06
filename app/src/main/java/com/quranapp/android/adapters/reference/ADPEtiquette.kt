package com.quranapp.android.adapters.reference

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.databinding.LytQuranEtiquetteVerseItemBinding
import com.quranapp.android.utils.gesture.HoverPushEffect
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect
import com.quranapp.android.utils.reader.factory.ReaderFactory

class ADPEtiquette(
    private val references: List<ExclusiveVerse>
) : RecyclerView.Adapter<ADPEtiquette.VHEtiquetteVerse>() {
    override fun getItemCount(): Int {
        return references.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHEtiquetteVerse {
        return VHEtiquetteVerse(
            LytQuranEtiquetteVerseItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHEtiquetteVerse, position: Int) {
        holder.bind(references[position])
    }

    inner class VHEtiquetteVerse(private val binding: LytQuranEtiquetteVerseItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.apply {
                setOnTouchListener(HoverPushOpacityEffect(HoverPushEffect.Pressure.LOW))
                setBackgroundResource(R.drawable.dr_bg_search_result_item)
            }
        }

        fun bind(verse: ExclusiveVerse) {
            binding.text.text = verse.name

            binding.root.setOnClickListener {
                // Take the first reference as it has only one verse
                val reference = verse.verses.first()
                ReaderFactory.startVerseRange(
                    it.context,
                    reference.first,
                    reference.second,
                    reference.third,
                )
            }
        }
    }
}