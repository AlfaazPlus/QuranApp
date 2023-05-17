package com.quranapp.android.adapters.reference

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.activities.reference.ActivityQuranScienceContent
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.databinding.LytQuranScienceItemBinding

class ADPQuranScience(private val items: List<QuranScienceItem>) :
    RecyclerView.Adapter<ADPQuranScience.VHQuranScience>() {
    inner class VHQuranScience(val binding: LytQuranScienceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: QuranScienceItem) {
            binding.image.setImageResource(item.drawableRes)
            binding.title.text = item.title

            val subtitle = "${item.referencesCount} references"
            binding.subTitle.text = subtitle

            binding.root.setOnClickListener {
                it.context.startActivity(Intent(it.context, ActivityQuranScienceContent::class.java).apply {
                    putExtra("item", item)
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHQuranScience {
        return VHQuranScience(LytQuranScienceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: VHQuranScience, position: Int) {
        holder.bind(items[position])
    }
}