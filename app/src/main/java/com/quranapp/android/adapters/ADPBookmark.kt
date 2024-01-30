/*
 * (c) Faisal Khan. Created on 28/9/2021.
 */
package com.quranapp.android.adapters

import android.content.res.ColorStateList
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
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityBookmark
import com.quranapp.android.adapters.extended.PeaceBottomSheetMenuAdapter
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytBookmarkItemBinding
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetMenu
import com.quranapp.android.widgets.list.base.BaseListItem
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class ADPBookmark(private val mActivity: ActivityBookmark, private var mBookmarkModels: ArrayList<BookmarkModel>) :
    RecyclerView.Adapter<ADPBookmark.VHBookmark>() {
    val mSelectedModels: MutableSet<BookmarkModel> = LinkedHashSet()
    private val mQuranMetaRef: AtomicReference<QuranMeta> = mActivity.quranMetaRef
    private val datetimeTxtSize: Int = mActivity.getDimenPx(R.dimen.dmnCommonSize3)
    private val datetimeColor: ColorStateList? = ContextCompat.getColorStateList(mActivity, R.color.colorText3)
    var mIsSelecting = false

    fun updateModels(models: ArrayList<BookmarkModel>) {
        mBookmarkModels = models
        clearSelection()
    }

    fun updateModel(model: BookmarkModel, position: Int) {
        try {
            mBookmarkModels[position] = model
            notifyItemChanged(position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeItemFromAdapter(position: Int) {
        mBookmarkModels.removeAt(position)
        notifyItemRemoved(position)

        if (itemCount == 0) {
            mActivity.noSavedItems()
        }
    }

    fun onSelectionChanged(position: Int, model: BookmarkModel, selected: Boolean) {
        if (mSelectedModels.isEmpty()) {
            mIsSelecting = true
            notifyDataSetChanged()
        }

        notifyItemChanged(position)

        if (selected) mSelectedModels.add(model)
        else mSelectedModels.remove(model)

        mActivity.onSelection(mSelectedModels.size)

        if (mSelectedModels.isEmpty()) {
            mIsSelecting = false
            notifyDataSetChanged()
        }
    }

    fun clearSelection() {
        mSelectedModels.clear()
        mIsSelecting = false
        notifyDataSetChanged()
        mActivity.onSelection(0)
    }

    override fun getItemCount(): Int {
        return mBookmarkModels.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHBookmark {
        val binding = LytBookmarkItemBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return VHBookmark(binding)
    }

    override fun onBindViewHolder(holder: VHBookmark, position: Int) {
        holder.bind(mBookmarkModels[position])
    }

    private fun prepareTexts(title: String, subTitle: CharSequence, datetime: String?): CharSequence {
        val titleSS = SpannableString(title)
        val titleTASpan = TextAppearanceSpan("sans-serif", Typeface.BOLD, -1, null, null)
        titleSS.setSpan(titleTASpan, 0, titleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val subTitleSS = SpannableString(subTitle)
        val subTitleTASpan = TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, -1, null, null)
        subTitleSS.setSpan(subTitleTASpan, 0, subTitleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        subTitleSS.setSpan(LineHeightSpan2(20, false, true), 0, subTitleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val datetimeSS = SpannableString(datetime)
        val datetimeTASpan = TextAppearanceSpan("sans-serif", Typeface.NORMAL, datetimeTxtSize, datetimeColor, null)
        datetimeSS.setSpan(datetimeTASpan, 0, datetimeSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", datetimeSS)
    }

    inner class VHBookmark(private val mBinding: LytBookmarkItemBinding) : RecyclerView.ViewHolder(mBinding.root) {
        init {
            mBinding.root.let {
                it.elevation = Dimen.dp2px(mBinding.root.context, 4f).toFloat()
                it.setBackgroundResource(R.drawable.dr_bg_chapter_card)
            }
        }

        fun bind(model: BookmarkModel) {
            val isSelected = mSelectedModels.contains(model)
            mBinding.chapterNo.visibility = if (isSelected) View.GONE else View.VISIBLE
            mBinding.menu.visibility = if (mIsSelecting) View.GONE else View.VISIBLE
            mBinding.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            mBinding.thumb.isSelected = isSelected

            if (!isSelected) {
                mBinding.chapterNo.text = String.format(Locale.getDefault(), "%d", model.chapterNo)
            }

            val chapterName = mQuranMetaRef.get().getChapterName(itemView.context, model.chapterNo, true)
            val txt = prepareTexts(chapterName,
                mActivity.prepareSubtitleTitle(model.fromVerseNo, model.toVerseNo),
                model.getFormattedDate(mActivity))
            mBinding.text.text = txt

            mBinding.root.setOnLongClickListener { v ->
                onSelectionChanged(bindingAdapterPosition, model, !mSelectedModels.contains(model))
                true
            }

            mBinding.root.setOnClickListener { v ->
                if (mIsSelecting) onSelectionChanged(bindingAdapterPosition, model, !mSelectedModels.contains(model))
                else mActivity.onView(model, bindingAdapterPosition)
            }

            mBinding.menu.setOnClickListener { v ->
                if (!mIsSelecting) {
                    val title: String = if (model.fromVerseNo == model.toVerseNo) {
                        String.format("%s %d:%d", chapterName, model.chapterNo, model.fromVerseNo)
                    } else {
                        String.format("%s %d:%d-%d", chapterName, model.chapterNo, model.fromVerseNo, model.toVerseNo)
                    }
                    openItemMenu(title, model)
                }
            }
        }

        private fun openItemMenu(title: String, model: BookmarkModel) {
            val dialog = PeaceBottomSheetMenu()
            dialog.params.headerTitle = title

            val ta = mActivity.typedArray(R.array.arrBookmarkItemMenuIcons)
            val labels = mActivity.strArray(R.array.arrBookmarkItemMenuLabels)
            val descs = mActivity.strArray(R.array.arrBookmarkItemMenuDescs)

            val adapter = PeaceBottomSheetMenuAdapter(mActivity)

            for (i in labels.indices){
                val label = labels[i]
                val item = BaseListItem(ta.getResourceId(i, 0), label)
                item.message = descs[i]
                item.position = i
                adapter.addItem(item)
            }

            dialog.adapter = adapter
            dialog.onItemClickListener = object : PeaceBottomSheetMenu.OnItemClickListener {
                override fun onItemClick(dialog: PeaceBottomSheetMenu, item: BaseListItem) {
                    dialog.dismiss()

                    when (item.position) {
                        0 -> mActivity.onView(model, bindingAdapterPosition)
                        1 -> mActivity.onOpen(model)
                        2 -> mActivity.removeVerseFromBookmark(model, bindingAdapterPosition)
                    }
                }
            }

            dialog.show(mActivity.supportFragmentManager)
        }
    }
}
