/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 24/7/2022.
 * All rights reserved.
 */
package com.quranapp.android.views.readerSpinner2.adapters

import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.databinding.LytJuzChapterVerseSheetBinding
import com.quranapp.android.views.reader.chapterSpinner.ChapterSpinnerItem
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem
import com.quranapp.android.views.readerSpinner2.juzChapterVerse.JuzChapterVerseSelector
import com.quranapp.android.views.readerSpinner2.viewholders.VHJuzChapterVerseBase
import java.util.regex.Pattern

abstract class ADPJuzChapterVerseBase<ITEM : ReaderSpinnerItem, VH : VHJuzChapterVerseBase<ITEM>>(
    items: MutableList<ITEM>
) : RecyclerView.Adapter<VH>() {
    private var storedItems: MutableList<ITEM> = ArrayList()
    private var mItems: MutableList<ITEM> = ArrayList()
    private var mSpinner: JuzChapterVerseSelector? = null
    var selectedItem: ITEM? = null

    init {
        setPositionToItems(items)
        storedItems = items
        mItems = items
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItem(position: Int): ITEM {
        return mItems[position]
    }

    fun getItemFromStoredList(position: Int): ITEM {
        return storedItems[position]
    }

    fun drainItems() {
        mItems.clear()
        storedItems.clear()
        notifyDataSetChanged()
    }

    private fun setPositionToItems(items: List<ITEM>) {
        items.forEachIndexed { index, item ->
            item.position = index
        }
    }

    fun setSpinner(spinner: JuzChapterVerseSelector) {
        mSpinner = spinner
    }

    fun onItemSelectInAdapter(item: ITEM, invokeListener: Boolean) {
        mSpinner?.onItemSelectInSpinner(item, invokeListener)

        selectedItem?.let {
            it.selected = false
            notifyItemChanged(it.position)
        }

        item.selected = true
        notifyItemChanged(item.position)
        selectedItem = item
    }

    fun selectSansInvocation(position: Int) {
        onItemSelectInAdapter(getItem(position), false)
    }

    private fun setSearchItems(items: MutableList<ITEM>) {
        mItems = items
        setPositionToItems(items)
        notifyDataSetChanged()
    }

    fun search(searchQuery: String, binding: LytJuzChapterVerseSheetBinding) {
        val nSearchQuery = Regex.escape(searchQuery)

        if (nSearchQuery.isEmpty()) {
            setSearchItems(storedItems)
            scrollToSelected(binding)
            return
        }

        val pattern = Pattern.compile(nSearchQuery, Pattern.CASE_INSENSITIVE)
        val foundItems = ArrayList<ITEM>()

        storedItems.forEach { item ->
            if (search(item, pattern)) {
                foundItems.add(item)
            }
        }

        setSearchItems(foundItems)
        if (this is ChapterSelectorAdapter2) {
            binding.juzChapterSec.list.scrollToPosition(0)
        } else if (this is VerseSelectorAdapter2) {
            binding.verseSec.list.scrollToPosition(0)
        }
    }

    private fun search(item: ITEM, pattern: Pattern): Boolean {
        if (item is ChapterSpinnerItem) {
            val chapter = item.chapter
            return pattern.matcher(chapter.chapterNumber.toString() + chapter.tags).find()
        }

        return pattern.matcher(item.label).find()
    }

    fun scrollToSelected(binding: LytJuzChapterVerseSheetBinding) {
        selectedItem?.let {
            if (this is ChapterSelectorAdapter2 || this is JuzSelectorAdapter2) {
                binding.juzChapterSec.list.scrollToPosition(it.position)
            } else if (this is VerseSelectorAdapter2) {
                binding.verseSec.list.scrollToPosition(it.position)
            }
        }
    }
}
