package com.quranapp.android.adapters.storageCleanup

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.components.storageCleanup.TranslationCleanupItemModel
import com.quranapp.android.components.transls.TranslBaseModel
import com.quranapp.android.components.transls.TranslTitleModel
import com.quranapp.android.databinding.LytTranslationCleanupItemBinding
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.*
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory

class ADPTranslationCleanup(ctx: Context, private val items: List<TranslBaseModel>) :
    RecyclerView.Adapter<ADPTranslationCleanup.VHTranslationCleanupItem>() {
    private val colorPrimary = ctx.color(R.color.colorPrimary)

    init {
        setHasStableIds(true)
        Log.d("ADPTranslationCleanup", "items: ${items.size}")
    }

    override fun getItemCount(): Int {
        return items.size
    }


    override fun getItemViewType(position: Int): Int {
        return if (items[position] is TranslTitleModel) 0 else 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHTranslationCleanupItem {
        return if (viewType == 0) {
            VHTranslationCleanupItem(createTitleView(parent.context))
        } else {
            VHTranslationCleanupItem(LytTranslationCleanupItemBinding.inflate(parent.getLayoutInflater(), parent, false))
        }
    }

    private fun createTitleView(context: Context): AppCompatTextView {
        return AppCompatTextView(context).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            updatePaddingHorizontal(context.dp2px(20f))
            setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)
            setTextColor(colorPrimary)

            layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                updateMarginVertical(context.dp2px(10f))
            }
        }
    }

    override fun onBindViewHolder(holder: VHTranslationCleanupItem, position: Int) {
        val model: TranslBaseModel = items[position]


        if (model is TranslTitleModel && holder.itemView is TextView) {
            (holder.itemView as TextView).text = model.langName
        } else if (model is TranslationCleanupItemModel) {
            holder.bind(model)
        }
    }

    inner class VHTranslationCleanupItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: LytTranslationCleanupItemBinding? = null

        constructor(binding: LytTranslationCleanupItemBinding) : this(binding.root) {
            this.binding = binding
        }

        fun bind(model: TranslationCleanupItemModel) {
            binding?.let {
                it.book.text = model.bookInfo.bookName
                it.author.text = model.bookInfo.authorName

                it.iconDelete.setImageResource(if (!model.isDeleted) R.drawable.dr_icon_delete else R.drawable.dr_icon_check)

                if (!model.isDeleted) {
                    it.iconDelete.setOnClickListener {
                        deleteItem(model)
                    }
                } else {
                    it.iconDelete.setOnClickListener(null)
                }
            }
        }

        private fun deleteItem(model: TranslationCleanupItemModel) {
            val bookInfo = model.bookInfo

            PeaceDialog.newBuilder(itemView.context).apply {
                setTitle(R.string.strTitleTranslDelete)
                setMessage(itemView.context.getString(R.string.msgDeleteTranslation, bookInfo.bookName, bookInfo.authorName))
                setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                setNeutralButton(R.string.strLabelCancel, null)
                setDialogGravity(PeaceDialog.GRAVITY_TOP)
                setNegativeButton(R.string.strLabelDelete, ColorUtils.DANGER) { _, _ ->
                    model.isDeleted = true
                    notifyItemChanged(adapterPosition)
                    /*QuranTranslationFactory(itemView.context).use {
                        it.deleteTranslation(bookInfo.slug)
                        model.isDeleted = true
                        notifyItemChanged(adapterPosition)
                    }*/
                }
                setFocusOnNegative(true)
            }.show()
        }
    }
}