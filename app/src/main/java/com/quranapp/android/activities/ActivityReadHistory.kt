package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.adapters.ADPReadHistory
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.databinding.ActivityBookmarkBinding
import com.quranapp.android.db.readHistory.ReadHistoryDBHelper
import com.quranapp.android.interfaceUtils.ReadHistoryCallbacks
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ActivityReadHistory : QuranMetaPossessingActivity(), ReadHistoryCallbacks {
    private lateinit var binding: ActivityBookmarkBinding
    private var readHistoryDBHelper: ReadHistoryDBHelper? = null
    private var adapter: ADPReadHistory? = null
    override fun onDestroy() {
        readHistoryDBHelper?.close()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (readHistoryDBHelper != null && adapter != null) {
            refreshHistories()
        }
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_bookmark
    }

    override fun shouldInflateAsynchronously(): Boolean {
        return true
    }

    override fun preActivityInflate(savedInstanceState: Bundle?) {
        readHistoryDBHelper = ReadHistoryDBHelper(this)
    }

    override fun onQuranMetaReady(
        activityView: View,
        intent: Intent,
        savedInstanceState: Bundle?,
        quranMeta: QuranMeta
    ) {
        binding = ActivityBookmarkBinding.bind(activityView)
        initHeader(binding.header)
        init()
    }

    private fun init() {
        binding.apply {
            noItemsIcon.setImageResource(R.drawable.dr_icon_history)
            noItemsText.setText(R.string.strMsgReadHistoryNoItems)
            loader.visibility = View.VISIBLE
        }
        refreshHistories()
    }

    private fun initHeader(header: BoldHeader) {
        header.apply {
            setCallback(object : BoldHeaderCallback {
                override fun onBackIconClick() {
                    onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    val totalItems = adapter?.itemCount ?: -1

                    MessageUtils.showConfirmationDialog(
                        context = this@ActivityReadHistory,
                        title = getString(R.string.msgClearReadHistory),
                        msg = if (totalItems > 1) getString(R.string.nItems, totalItems)
                        else getString(R.string.nItem, totalItems),
                        btn = getString(R.string.strLabelRemoveAll),
                        btnColor = ColorUtils.DANGER,
                    ) {
                        readHistoryDBHelper?.deleteAllHistories()
                        refreshHistories()
                    }
                }
            })
            setTitleText(R.string.strTitleReadHistory)
            setLeftIconRes(R.drawable.dr_icon_arrow_left)
            setShowRightIcon(false)
            setRightIconRes(R.drawable.dr_icon_delete, getString(R.string.msgClearReadHistory))
            setBGColor(R.color.colorBGPage)
        }
    }

    private fun refreshHistories() {
        CoroutineScope(Dispatchers.IO).launch {
            val models = readHistoryDBHelper?.getAllHistories(-1) ?: ArrayList()

            withContext(Dispatchers.Main) {
                setupAdapter(ArrayList(models))

                binding.loader.visibility = View.GONE
            }
        }
    }

    private fun setupAdapter(models: ArrayList<ReadHistoryModel>) {
        if (adapter == null && models.isNotEmpty()) {
            val spanCount = if (WindowUtils.isLandscapeMode(this)) 2 else 1
            binding.list.layoutManager = GridLayoutManager(this, spanCount)

            adapter = ADPReadHistory(this, mQuranMetaRef.get(), models, ViewGroup.LayoutParams.MATCH_PARENT)

            binding.list.adapter = adapter
        } else {
            adapter?.updateModels(models)
        }

        binding.header.setShowRightIcon(models.isNotEmpty())
        binding.noItems.visibility = if (models.isEmpty()) View.VISIBLE else View.GONE

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                val position = viewHolder.bindingAdapterPosition

                if (position < 0 || position >= models.size) {
                    return
                }

                val model = models[position]
                readHistoryDBHelper?.deleteHistory(model)
                models.removeAt(position)
                adapter?.notifyItemRemoved(position)

                if (models.isEmpty()) {
                    refreshHistories()
                }
            }
        })
        touchHelper.attachToRecyclerView(binding.list)
    }

    override fun onReadHistoryRemoved(model: ReadHistoryModel) {}
    override fun onReadHistoryAdded(model: ReadHistoryModel) {}
}