package com.quranapp.android.adapters.storageCleanup

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.components.storageCleanup.RecitationCleanupItemModel
import com.quranapp.android.databinding.LytStorageCleanupItemBinding
import com.quranapp.android.utils.extensions.layoutInflater
import com.quranapp.android.utils.univ.FileUtils
import java.io.File

class ADPRecitationCleanup(
    private val items: List<RecitationCleanupItemModel>,
    private val fileUtils: FileUtils
) :
    RecyclerView.Adapter<ADPRecitationCleanup.VHRecitationCleanupItem>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHRecitationCleanupItem {
        return VHRecitationCleanupItem(
            LytStorageCleanupItemBinding.inflate(
                parent.layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHRecitationCleanupItem, position: Int) {
        holder.bind(items[position])
    }

    inner class VHRecitationCleanupItem(
        private val binding: LytStorageCleanupItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: RecitationCleanupItemModel) {
            binding.let {
                it.title.text = model.recitationModel.getReciterName()
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

        private fun deleteItem(model: RecitationCleanupItemModel) {
            PeaceDialog.newBuilder(itemView.context).apply {
                setTitle(R.string.titleRecitationCleanup)
                setMessage(
                    itemView.context.getString(
                        R.string.msgRecitationCleanup,
                        model.recitationModel.getReciterName()
                    )
                )
                setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setNeutralButton(R.string.strLabelCancel, null)
                setDialogGravity(PeaceDialog.GRAVITY_TOP)
                setNegativeButton(R.string.strLabelDelete, ColorUtils.DANGER) { _, _ ->
                    File(fileUtils.recitationDir, model.recitationModel.slug).deleteRecursively()
                    model.isCleared = true
                    notifyItemChanged(bindingAdapterPosition)
                }
                setFocusOnNegative(true)
            }.show()
        }
    }
}
