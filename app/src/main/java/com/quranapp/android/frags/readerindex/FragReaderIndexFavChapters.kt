package com.quranapp.android.frags.readerindex

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.adapters.quranIndex.ADPFavChaptersList
import com.quranapp.android.utils.extensions.removeView
import com.quranapp.android.views.helper.RecyclerView2
import com.quranapp.android.widgets.PageAlert

class FragReaderIndexFavChapters : BaseFragReaderIndex() {
    override fun initList(list: RecyclerView2, ctx: Context) {
        super.initList(list, ctx)

        mHandler.post {
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
        val items = arrayListOf<Int>()

        mHandler.post {
            if (items.isEmpty()) {
                PageAlert(list.context).apply {
                    setMessage(R.string.noItems, R.string.msgNoFavouriteChapters)
                    show(list.parent as ViewGroup)
                    list.removeView()
                }
            } else {
                val adapter = ADPFavChaptersList(this, items)
                list.adapter = adapter
            }
        }
    }
}