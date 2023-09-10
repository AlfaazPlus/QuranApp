package com.quranapp.android.views.homepage

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReadHistory
import com.quranapp.android.adapters.ADPReadHistory
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytHomepageTitledItemTitleBinding
import com.quranapp.android.db.readHistory.ReadHistoryDBHelper
import com.quranapp.android.interfaceUtils.Destroyable
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.removeView
import com.quranapp.android.utils.extensions.setTextColorResource
import com.quranapp.android.utils.extensions.setTextSizePx
import com.quranapp.android.utils.extensions.updatePaddings
import com.quranapp.android.views.homepage2.HomepageCollectionLayoutBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadHistoryLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HomepageCollectionLayoutBase(context, attrs, defStyleAttr), Destroyable {
    private var dbHelper: ReadHistoryDBHelper? = null
    private var adapter: ADPReadHistory? = null
    private var firstTime = true
    override fun destroy() {
        dbHelper?.close()
    }

    override fun getHeaderTitle(): Int {
        return R.string.strTitleReadHistory;
    }

    override fun getHeaderIcon(): Int {
        return R.drawable.dr_icon_history;
    }

    override fun initialize() {
        super.initialize()

        dbHelper = ReadHistoryDBHelper(context)
    }

    override fun setupHeader(header: LytHomepageTitledItemTitleBinding) {
        header.titleIcon.setColorFilter(context.color(R.color.colorPrimary))
    }

    override fun onViewAllClick() {
        context.startActivity(Intent(context, ActivityReadHistory::class.java))
    }

    override fun refresh(quranMeta: QuranMeta) {
        if (firstTime) {
            showLoader()
            firstTime = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            val histories = dbHelper?.getAllHistories(10) ?: return@launch

            withContext(Dispatchers.Main) {
                hideLoader()
                if (histories.isEmpty()) {
                    makeNoHistoryAlert()
                } else {
                    if (adapter == null) {
                        adapter = ADPReadHistory(context, quranMeta, histories, Dimen.dp2px(context, 280f))
                        resolveListView().adapter = adapter
                    } else {
                        adapter!!.updateModels(histories)
                    }
                }
            }
        }
    }

    private fun makeNoHistoryAlert() {
        findViewById<View>(R.id.list).removeView()
        adapter = null

        if (findViewById<View?>(R.id.text) != null) {
            return
        }

        addView(AppCompatTextView(context).apply {
            id = R.id.text
            updatePaddings(context.dp2px(20f), context.dp2px(15f))
            setTextSizePx(R.dimen.dmnCommonSize1_5)
            setTextColorResource(R.color.colorText2)
            setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setText(R.string.strMsgReadShowupHere)
        })
    }

    override fun resolveListView(): RecyclerView {
        findViewById<View>(R.id.text).removeView()
        return super.resolveListView()
    }

}