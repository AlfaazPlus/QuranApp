package com.quranapp.android.widgets.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.quranapp.android.widgets.list.base.BaseListAdapter
import com.quranapp.android.widgets.list.base.BaseListItem
import com.quranapp.android.widgets.list.base.BaseListView
import com.quranapp.android.widgets.list.singleChoice.SingleChoiceListAdapter
import com.quranapp.android.widgets.list.singleChoice.SingleChoiceListView

open class PeaceBottomSheetMenu : PeaceBottomSheet() {
    var onItemClickListener: OnItemClickListener? = null
    var adapter: BaseListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If savedInstanceState != null, it means onCreate was called after a onSavedInstanceState.
        // So we will dismiss the menu because we are currently not supporting state saving.
        if (savedInstanceState != null) {
            try {
                dismiss()
            } catch (ignored: Exception) {
            }
        }
    }

    override fun setupContentView(dialogLayout: LinearLayout, params: PeaceBottomSheetParams) {
        if (adapter == null) return

        dialogLayout.addView(createAdapterView(dialogLayout.context, adapter!!))
    }

    protected open fun createAdapterView(context: Context, listAdapter: BaseListAdapter): View {
        val listView = if (listAdapter is SingleChoiceListAdapter) {
            SingleChoiceListView(context)
        } else {
            BaseListView(context)
        }

        listView.setOnItemClickListener(object : BaseListView.OnItemClickListener {
            override fun onItemClick(item: BaseListItem) {
                onItemClickListener?.onItemClick(this@PeaceBottomSheetMenu, item)
            }
        })

        listView.post { listView.setAdapter(adapter) }

        return listView
    }

    interface OnItemClickListener {
        fun onItemClick(dialog: PeaceBottomSheetMenu, item: BaseListItem)
    }
}
