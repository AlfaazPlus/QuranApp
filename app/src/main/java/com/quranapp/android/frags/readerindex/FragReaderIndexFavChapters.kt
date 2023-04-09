package com.quranapp.android.frags.readerindex

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.adapters.quranIndex.ADPFavChaptersList
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters
import com.quranapp.android.views.helper.RecyclerView2
import com.quranapp.android.widgets.PageAlert

class FragReaderIndexFavChapters : BaseFragReaderIndex() {
    private var pageAlert: PageAlert? = null
    private var items = arrayListOf<Int>()

    override fun onResume() {
        super.onResume()

        if (job?.isCompleted != false) {
            resetAdapter(binding.list, binding.list.context, false)
        }
    }

    override fun initList(list: RecyclerView2, ctx: Context) {
        super.initList(list, ctx)

        list.post {
            list.layoutManager = GridLayoutManager(
                ctx,
                if (WindowUtils.isLandscapeMode(ctx)) 2 else 1,
                RecyclerView.VERTICAL,
                false
            )
        }

        resetAdapter(list, ctx, false)
    }

    override fun resetAdapter(list: RecyclerView2, ctx: Context, reverse: Boolean) {
        super.resetAdapter(list, ctx, reverse)
        val newList = ArrayList(SPFavouriteChapters.getFavouriteChapters(ctx))

        if (items.isNotEmpty() && newList == items) return

        items = newList

        activity?.runOnUiThread {
            if (items.isEmpty()) {
                noItems(ctx, list)
            } else {
                val adapter = ADPFavChaptersList(this, items)
                list.adapter = adapter
                list.visibility = View.VISIBLE
                pageAlert?.remove()
            }
        }
    }

    private fun noItems(ctx: Context, list: RecyclerView2) {
        pageAlert = PageAlert(ctx).apply {
            setMessage(R.string.noItems, R.string.msgNoFavouriteChapters)
            show(list.parent as ViewGroup)
            list.visibility = View.GONE
        }
    }

    fun removeFromFavorites(context: Context, chapterNo: Int, adapterPosition: Int) {
        SPFavouriteChapters.removeFromFavorites(context, chapterNo)
        items.removeAt(adapterPosition)
        binding.list.adapter?.notifyItemRemoved(adapterPosition)

        if (items.isEmpty()) {
            noItems(context, binding.list)
        }
    }
}