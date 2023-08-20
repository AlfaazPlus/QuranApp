/*
 * Created by Faisal Khan on (c) 13/8/2021.
 */
package com.quranapp.android.activities.reference

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.activities.QuranMetaPossessingActivity
import com.quranapp.android.adapters.ADPProphets
import com.quranapp.android.adapters.utility.TopicFilterSpinnerAdapter
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.components.utility.SpinnerItem
import com.quranapp.android.databinding.ActivityTopicsBinding
import com.quranapp.android.databinding.LytTopicsActivityHeaderBinding
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.simplified.SimpleTextWatcher
import com.quranapp.android.views.helper.Spinner2
import com.quranapp.android.views.helper.Spinner2.SimplerSpinnerItemSelectListener
import java.util.regex.Pattern

class ActivityProphets : QuranMetaPossessingActivity() {
    private lateinit var binding: ActivityTopicsBinding
    private val searchHandler = Handler(Looper.getMainLooper())
    private var prophetsAdapter: ADPProphets? = null

    override fun getStatusBarBG() = color(R.color.colorBGHomePageItem)

    override fun shouldInflateAsynchronously() = true

    override fun getLayoutResource() = R.layout.activity_topics

    override fun preQuranMetaPrepare(activityView: View, intent: Intent, savedInstanceState: Bundle?) {
        binding = ActivityTopicsBinding.bind(activityView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.header.searchContainer.root.visibility == View.VISIBLE) {
                    toggleSearchBox(binding.header, false)
                } else {
                    this.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onQuranMetaReady(
        activityView: View,
        intent: Intent,
        savedInstanceState: Bundle?,
        quranMeta: QuranMeta
    ) {
        QuranProphet.prepareInstance(this, quranMeta, this::initContent)
    }

    private fun initContent(quranProphet: QuranProphet) {
        initTopics()
        initHeader(binding.header, quranProphet)
    }

    private fun initHeader(header: LytTopicsActivityHeaderBinding, quranProphet: QuranProphet) {
        header.searchContainer.root.setBackgroundColor(statusBarBG)
        header.topicTitle.text = getString(R.string.strTitleProphets)
        initProphetFilters(header.filter, quranProphet)

        header.search.setOnClickListener { toggleSearchBox(header, true) }
        header.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        header.searchContainer.searchBox.let {
            header.searchContainer.btnClear.setOnClickListener view@{ header.searchContainer.searchBox.text = null }
            it.updatePadding(dp2px(5f))
            it.onFocusChangeListener = OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (!hasFocus) {
                    val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
            it.addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    binding.header.searchContainer.btnClear.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
                    searchHandler.removeCallbacksAndMessages(null)
                    searchHandler.postDelayed({ searchProphets(s.toString(), quranProphet) }, 150)
                }
            })
        }
    }

    private fun initProphetFilters(spinner: Spinner2, quranProphet: QuranProphet) {
        val filters: MutableList<SpinnerItem> = ArrayList()
        val items = strArray(R.array.arrProphetFilterItems)
        for (item in items) {
            filters.add(SpinnerItem(item))
        }
        val itemLayoutRes = R.layout.lyt_topic_filter_spinner_item
        val adapter = TopicFilterSpinnerAdapter(
            spinner.context, itemLayoutRes,
            R.id.text, filters
        )
        spinner.setAdapterWithDynamicWidth(adapter)

        // running it twice before and after setting listener to prevent it to be invoked for the first time.
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : SimplerSpinnerItemSelectListener() {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (position == 0) {
                    resetAdapter(quranProphet.prophets.sortedBy { it.name })
                } else if (position == 1) {
                    resetAdapter(quranProphet.prophets.sortedBy { it.order })
                    Toast.makeText(this@ActivityProphets, R.string.strMsgProphetsOrder, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // running it twice before and after setting listener to prevent it to be invoked for the first time.
        spinner.setSelection(0)
    }

    private fun searchProphets(query: String, quranProphet: QuranProphet) {
        if (TextUtils.isEmpty(query)) {
            resetAdapter(quranProphet.prophets)
            return
        }

        val pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE or Pattern.LITERAL or Pattern.DOTALL)
        val queryProphets: MutableList<QuranProphet.Prophet> = ArrayList()

        for (prophet in quranProphet.prophets) {
            val matcher = pattern.matcher(prophet.name + prophet.nameEn + prophet.nameAr)
            if (matcher.find()) {
                queryProphets.add(prophet)
            }
        }

        resetAdapter(queryProphets)
    }

    private fun toggleSearchBox(header: LytTopicsActivityHeaderBinding, showSearch: Boolean) {
        header.searchContainer.root.visibility = if (showSearch) View.VISIBLE else View.GONE
        header.searchContainer.searchBox.let {
            it.setHint(R.string.strHintSearchProphet)
            if (showSearch) {
                it.requestFocus()
                ContextCompat.getSystemService(this, InputMethodManager::class.java)
                    ?.showSoftInput(it, 0)
            } else {
                it.clearFocus()
                it.text = null
            }
        }
    }

    private fun initTopics() {
        prophetsAdapter = ADPProphets(this, ViewGroup.LayoutParams.MATCH_PARENT, -1)
        binding.topics.let {
            it.addItemDecoration(GapedItemDecoration(dp2px(2.5f)))
            (it.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = prophetsAdapter
        }
    }

    private fun resetAdapter(prophets: List<QuranProphet.Prophet>) {
        prophetsAdapter?.let {
            it.prophets = prophets
            it.notifyDataSetChanged()
        }
    }
}