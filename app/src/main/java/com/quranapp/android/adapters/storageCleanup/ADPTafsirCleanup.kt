package com.quranapp.android.adapters.storageCleanup

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.components.storageCleanup.TafsirCleanupItemModel
import com.quranapp.android.databinding.LytStorageCleanupItemBinding
import com.quranapp.android.utils.extensions.layoutInflater
import com.quranapp.android.utils.univ.FileUtils
import java.io.File

class ADPTafsirCleanup(
    private val items: List<TafsirCleanupItemModel>,
    private val fileUtils: FileUtils
) : RecyclerView.Adapter<ADPTafsirCleanup.VHTafsirCleanupItem>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHTafsirCleanupItem {
        return VHTafsirCleanupItem(
            LytStorageCleanupItemBinding.inflate(
                parent.layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHTafsirCleanupItem, position: Int) {
        holder.bind(items[position])
    }

    inner class VHTafsirCleanupItem(
        private val binding: LytStorageCleanupItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: TafsirCleanupItemModel) {
            binding.let {
                it.title.text = model.tafsirModel.name
                it.subtitle.text = it.root.context.getString(R.string.nFiles, model.downloadsCount)

                it.iconDelete.setImageResource(
                    if (!model.isCleared) R.drawable.dr_icon_delete else R.drawable.dr_icon_check
                )

                if (!model.isCleared) {
                    it.iconDelete.setOnClickListener {
                        deleteItem(model)
                    }
                } else {
                    it.iconDelete.setOnClickListener(null)
                }

                it.iconDelete.isClickable = !model.isCleared
                it.iconDelete.isFocusable = !model.isCleared
            }
        }

        private fun deleteItem(model: TafsirCleanupItemModel) {
            PeaceDialog.newBuilder(itemView.context).apply {
                setTitle(R.string.titleTafsirCleanup)
                setMessage(
                    itemView.context.getString(
                        R.string.msgTafsirCleanup,
                        model.tafsirModel.name
                    )
                )
                setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setNeutralButton(R.string.strLabelCancel, null)
                setDialogGravity(PeaceDialog.GRAVITY_TOP)
                setNegativeButton(R.string.strLabelDelete, ColorUtils.DANGER) { _, _ ->
                    File(fileUtils.tafsirDir, model.tafsirModel.key).deleteRecursively()
                    model.isCleared = true
                    notifyItemChanged(bindingAdapterPosition)
                }
                setFocusOnNegative(true)
            }.show()
        }
    }
}
