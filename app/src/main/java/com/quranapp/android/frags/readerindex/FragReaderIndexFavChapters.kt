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

        val adapter = ADPFavChaptersList(this)
        list.adapter = adapter
        list.visibility = View.GONE

        updateAdapter(ctx, list, adapter)

        favChaptersModel.favChapters.observe(viewLifecycleOwner) {
            updateAdapter(ctx, list, adapter)
        }
    }

    private fun updateAdapter(ctx: Context, list: RecyclerView2, adapter: ADPFavChaptersList) {
        val items = ArrayList(SPFavouriteChapters.getFavouriteChapters(ctx))

        if (items.isEmpty()) {
            noItems(ctx, list)
        } else {
            adapter.chapterNos = items
            list.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    private fun noItems(ctx: Context, list: RecyclerView2) {
        pageAlert = PageAlert(ctx).apply {
            setMessage(R.string.noItems, R.string.msgNoFavouriteChapters)
            show(list.parent as ViewGroup)
            list.visibility = View.GONE
        }
    }
}