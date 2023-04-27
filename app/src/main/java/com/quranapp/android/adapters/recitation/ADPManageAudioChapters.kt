package com.quranapp.android.adapters.recitation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.components.recitation.ManageAudioChapterModel
import com.quranapp.android.databinding.LytManageAudioChapterItemBinding
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudioReciter
import com.quranapp.android.utils.extensions.color

class ADPManageAudioChapters(
    private val frag: FragSettingsManageAudioReciter,
    val chapters: List<ManageAudioChapterModel>
) :
    RecyclerView.Adapter<ADPManageAudioChapters.VHChapter>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChapter {
        return VHChapter(LytManageAudioChapterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VHChapter, position: Int) {
        holder.bind(chapters[position])
    }

    override fun getItemCount() = chapters.size

    override fun getItemId(position: Int) = position.toLong()

    inner class VHChapter(private val binding: LytManageAudioChapterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ManageAudioChapterModel) {
            val ctx = binding.root.context

            binding.serial.text = String.format("%d", model.chapterMeta.chapterNo)
            binding.title.text = model.title

            binding.iconDownload.let {
                it.visibility = if (model.downloading) View.GONE else View.VISIBLE
                it.setImageResource(if (model.downloaded) R.drawable.dr_icon_delete else R.drawable.dr_icon_download)
                it.setColorFilter(if (model.downloaded) ctx.color(R.color.colorDanger) else ctx.color(R.color.colorIcon))
            }
            binding.loader.visibility = if (model.downloading) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                if (model.downloading) return@setOnClickListener

                if (model.downloaded) {
                    frag.deleteDownloaded(model, bindingAdapterPosition)
                } else {
                    frag.initDownload(model, bindingAdapterPosition)
                }
            }
        }
    }
}