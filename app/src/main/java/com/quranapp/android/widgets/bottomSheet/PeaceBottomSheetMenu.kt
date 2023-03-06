package com.quranapp.android.widgets.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.peacedesign.android.widget.list.base.BaseListAdapter
import com.peacedesign.android.widget.list.base.BaseListItem
import com.peacedesign.android.widget.list.simple.SimpleListView
import com.peacedesign.android.widget.list.singleChoice.SingleChoiceListAdapter
import com.peacedesign.android.widget.list.singleChoice.SingleChoiceListView

open class PeaceBottomSheetMenu : PeaceBottomSheet() {
    var onItemClickListener: OnItemClickListener? = null
    var adapter: BaseListAdapter<out BaseListItem>? = null


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

    protected open fun createAdapterView(context: Context, listAdapter: BaseListAdapter<out BaseListItem>): View {
        val listView = if (listAdapter is SingleChoiceListAdapter) {
            SingleChoiceListView(context)
        } else {
            SimpleListView(context)
        }

        listView.setOnItemClickListener { item ->
            onItemClickListener?.onItemClick(this, item)
        }

        listView.post { listView.adapter = adapter }

        return listView
    }


    interface OnItemClickListener {
        fun onItemClick(dialog: PeaceBottomSheetMenu, item: BaseListItem)
    }
}