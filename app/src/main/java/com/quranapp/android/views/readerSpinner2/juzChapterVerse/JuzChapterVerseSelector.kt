/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 24/7/2022.
 * All rights reserved.
 */
package com.quranapp.android.views.readerSpinner2.juzChapterVerse

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytJuzChapterVerseSheetBinding
import com.quranapp.android.databinding.LytReaderIndexTabBinding
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.utils.extensions.*
import com.quranapp.android.utils.simplified.SimpleTabSelectorListener
import com.quranapp.android.utils.simplified.SimpleTextWatcher
import com.quranapp.android.utils.univ.RegexPattern
import com.quranapp.android.views.reader.ChapterIcon
import com.quranapp.android.views.reader.chapterSpinner.ChapterSpinnerItem
import com.quranapp.android.views.reader.juzSpinner.JuzSpinnerItem
import com.quranapp.android.views.reader.sheet.JuzChapterVerseSheet
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem
import com.quranapp.android.views.reader.verseSpinner.VerseSpinnerItem
import com.quranapp.android.views.readerSpinner2.adapters.ADPJuzChapterVerseBase
import com.quranapp.android.views.readerSpinner2.adapters.ChapterSelectorAdapter2
import com.quranapp.android.views.readerSpinner2.adapters.JuzSelectorAdapter2
import com.quranapp.android.views.readerSpinner2.adapters.VerseSelectorAdapter2
import com.quranapp.android.widgets.IconedTextView
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet

class JuzChapterVerseSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : IconedTextView(context, attrs, defStyleAttr) {
    private var mPopupBinding: LytJuzChapterVerseSheetBinding? = null

    private val mPopup = JuzChapterVerseSheet().apply {
        onDismissListener = object : PeaceBottomSheet.OnPeaceBottomSheetDismissListener {
            override fun onDismissed() {
                mPopupBinding?.let {
                    it.juzChapterSec.search.setText("")
                    it.verseSec.search.setText("")
                }
            }
        }
    }

    var juzOrChapterAdapter: ADPJuzChapterVerseBase<*, *>? = null
    private var juzOrChapterItemSelectListener: OnItemSelectedListener? = null
    private var tempJuzOrChapterItemSelectListener: OnItemSelectedListener? = null

    var verseAdapter: VerseSelectorAdapter2? = null
    var verseItemSelectListener: ((ReaderSpinnerItem) -> Unit)? = null

    var juzIconView: TextView? = null
    var chapterIconView: ChapterIcon? = null
    var activity: ActivityReader? = null

    init {
        initThis()
        initBottomSheetView(context)
    }

    private fun initThis() {
        gravity = Gravity.CENTER
        updatePaddingRelative(start = context.dp2px(6F), end = 0)

        setTextSize(TypedValue.COMPLEX_UNIT_SP, context.getDimenSp(R.dimen.dmnCommonSize3))

        val txtColor = context.color(R.color.colorIcon)
        setTextColor(txtColor)
        includeFontPadding = false

        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(txtColor))
        setDrawableEndDimen(context.dp2px(20F))
        setDrawables(null, null, context.drawable(R.drawable.dr_icon_arrow_drop_down), null)
        compoundDrawablePadding = context.dp2px(0F)
    }

    private fun initBottomSheetView(context: Context) {
        AsyncLayoutInflater(context).inflate(R.layout.lyt_juz_chapter_verse_sheet, null) { view, _, _ ->
            mPopupBinding = LytJuzChapterVerseSheetBinding.bind(view)
            mPopupBinding?.let {
                initTabs(it)
                initRecyclerViews(it, context)
                setupVerseHeaders(it)
                setupPopupDimensions(it)
                mPopup.params.contentView = it.root

                if (mPopup.isShowing()) {
                    view.removeView()
                    mPopup.getDialogLayout().addView(view)
                }

                if (juzOrChapterAdapter != null && juzOrChapterItemSelectListener != null) {
                    setJuzOrChapterAdapter(juzOrChapterAdapter!!, juzOrChapterItemSelectListener!!)
                }
                if (verseAdapter != null && verseItemSelectListener != null) {
                    setVerseAdapter(verseAdapter!!, verseItemSelectListener!!)
                }
            }
        }
    }

    private fun initTabs(binding: LytJuzChapterVerseSheetBinding) {
        val labels = arrayOf(R.string.strTitleReaderChapters, R.string.strTitleReaderJuz)
        labels.forEach { labelRes ->
            val tab = binding.tabLayout.newTab()

            val tabBinding = LytReaderIndexTabBinding.inflate(LayoutInflater.from(context))
            tab.customView = tabBinding.root
            tabBinding.tabTitle.setText(labelRes)

            binding.tabLayout.addTab(tab)
        }

        binding.tabLayout.addOnTabSelectedListener(object : SimpleTabSelectorListener() {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tempJuzOrChapterItemSelectListener = null

                val isAlternateTab = (tab.position == 0 && juzOrChapterAdapter !is ChapterSelectorAdapter2) ||
                        (tab.position == 1 && juzOrChapterAdapter !is JuzSelectorAdapter2)

                activity?.let {
                    if (isAlternateTab) {
                        if (tab.position == 0) {
                            prepareAndSetChapterAdapter(it, true)
                        }
                        if (tab.position == 1) {
                            prepareAndSetJuzAdapter(it, true)
                        }

                        binding.verseSec.root.disableView(true)
                        mPopupBinding?.verseSec?.search?.disableView(true)
                    } else {
                        cleanTempAdapter()
                    }
                }
            }
        })
    }

    private fun selectTab(
        binding: LytJuzChapterVerseSheetBinding,
        adapter: ADPJuzChapterVerseBase<*, *>
    ) {
        var tab: TabLayout.Tab? = null
        if (adapter is ChapterSelectorAdapter2) {
            tab = binding.tabLayout.getTabAt(0)
        } else if (adapter is JuzSelectorAdapter2) {
            tab = binding.tabLayout.getTabAt(1)
        }
        binding.tabLayout.selectTab(tab)
    }

    private fun initRecyclerViews(binding: LytJuzChapterVerseSheetBinding, context: Context) {
        binding.juzChapterSec.list.setHasFixedSize(true)
        binding.verseSec.list.setHasFixedSize(true)

        binding.juzChapterSec.list.layoutManager = LinearLayoutManager(context)
        binding.verseSec.list.layoutManager = LinearLayoutManager(context)

        binding.verseSec.list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return tempJuzOrChapterItemSelectListener != null
            }
        })
    }

    private fun setupJuzOrChapterHeader(
        binding: LytJuzChapterVerseSheetBinding,
        adapter: ADPJuzChapterVerseBase<*, *>
    ) {
        val titleRes =
            if (adapter is JuzSelectorAdapter2) R.string.strTitleReaderJuz2 else R.string.strTitleReaderChapters2
        val hintRes = if (adapter is JuzSelectorAdapter2) R.string.strHintSearchJuz else R.string.strHintSearchChapter
        binding.juzChapterSec.title.setText(titleRes)
        binding.juzChapterSec.search.setHint(hintRes)
        binding.juzChapterSec.search.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                adapter.search(s.toString(), binding)
            }
        })

        selectTab(binding, adapter)
    }

    private fun setupVerseHeaders(binding: LytJuzChapterVerseSheetBinding) {
        binding.verseSec.title.setText(R.string.strTitleReaderVerses)
        binding.verseSec.search.setHint(R.string.strHintSearch)
        binding.verseSec.search.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                verseAdapter?.search(s.toString(), binding)
            }
        })
        binding.verseSec.search.imeOptions = EditorInfo.IME_ACTION_GO
        binding.verseSec.search.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                try {
                    navigateToVerse(v.text)
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
                closePopup()
            }
            true
        }
    }

    @Throws(NumberFormatException::class)
    private fun navigateToVerse(searchQuery: CharSequence) {
        if (activity == null) {
            return
        }

        if (activity!!.mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ) {
            val m = RegexPattern.VERSE_JUMP_PATTERN.matcher(searchQuery)
            if (m.find()) {
                val r = m.toMatchResult()
                if (r.groupCount() >= 2) {
                    val chapNo = r.group(1).toInt()
                    val verseNo = r.group(2).toInt()
                    activity!!.handleVerseSpinnerSelectedVerseNo(chapNo, verseNo)
                }
            }
        } else {
            val chapter = activity!!.mReaderParams.currChapter ?: return
            activity!!.handleVerseSpinnerSelectedVerseNo(
                chapter.chapterNumber,
                searchQuery.toString().toInt()
            )
        }
    }

    private fun setSelectorText(spinnerText: CharSequence?) {
        val hasText = !spinnerText.isNullOrEmpty()
        if (hasText) {
            text = spinnerText
            visibility = VISIBLE
        } else {
            visibility = GONE
        }
    }

    private fun setSelectorTextInternal(spinnerItem: ReaderSpinnerItem) {
        var text: CharSequence = ""
        var delimiter = ""
        if (spinnerItem is ChapterSpinnerItem || spinnerItem is JuzSpinnerItem) {
            if (spinnerItem is ChapterSpinnerItem) {
                text = spinnerItem.label
                delimiter = " : "
            }

            if (spinnerItem is ChapterSpinnerItem && chapterIconView != null) {
                chapterIconView!!.setChapterNumber(spinnerItem.chapter.chapterNumber)
            } else if (spinnerItem is JuzSpinnerItem && juzIconView != null) {
                juzIconView!!.text = spinnerItem.label
            }

            findSelectedItemForText(verseAdapter)?.let { item ->
                val verseItem = item as VerseSpinnerItem
                text = TextUtils.concat(text, delimiter, verseItem.label)
            }
        } else if (spinnerItem is VerseSpinnerItem) {
            var verseText = spinnerItem.label

            findSelectedItemForText(juzOrChapterAdapter)?.let { juzOrChapterItem ->
                delimiter = " : "

                if (juzOrChapterItem is ChapterSpinnerItem) {
                    text = juzOrChapterItem.label
                } else {
                    text = activity?.mQuranMetaRef?.get()?.getChapterName(
                        context,
                        spinnerItem.chapterNo
                    ) ?: ""
                    verseText = context.getString(R.string.strLabelVerseNo, spinnerItem.verseNo)
                }
            }

            text = TextUtils.concat(text, delimiter, verseText)
        }

        setSelectorText(text)
    }

    private fun beforeShow() {
        cleanTempAdapter()
        scrollToCurrent()
    }

    private fun scrollToCurrent() {
        scrollToCurrentJuzOrChapter()
        scrollToCurrentVerse()
    }

    private fun scrollToCurrentJuzOrChapter() {
        val isTemporaryJuzOrChapterAdapter = tempJuzOrChapterItemSelectListener != null
        if (!isTemporaryJuzOrChapterAdapter) {
            mPopupBinding?.let { juzOrChapterAdapter?.scrollToSelected(it) }
        }
    }

    private fun scrollToCurrentVerse() {
        mPopupBinding?.let { verseAdapter?.scrollToSelected(it) }
    }

    private fun setupPopupDimensions(binding: LytJuzChapterVerseSheetBinding) {
        binding.let {
            var height = context.getWindowHeight()
            if (height >= context.dp2px(70f)) {
                height = context.dp2px(650f)
            }
            it.root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }

    fun showPopup() {
        beforeShow()

        activity?.let {
            mPopup.show(it.supportFragmentManager)
        }
    }

    fun closePopup() {
        mPopup.dismiss()
    }

    fun onItemSelectInSpinner(item: ReaderSpinnerItem, invokeListener: Boolean) {
        setSelectorTextInternal(item)

        if (invokeListener) {
            if (item is JuzSpinnerItem || item is ChapterSpinnerItem) {
                if (tempJuzOrChapterItemSelectListener != null) {
                    tempJuzOrChapterItemSelectListener!!.onItemSelect(item)
                } else if (juzOrChapterItemSelectListener != null) {
                    juzOrChapterItemSelectListener!!.onItemSelect(item)
                }
            } else if (item is VerseSpinnerItem && verseItemSelectListener != null) {
                verseItemSelectListener!!(item)
            }
            closePopup()
        } else {
            scrollToCurrent()
        }
    }

    private fun cleanTempAdapter() {
        tempJuzOrChapterItemSelectListener = null

        if (juzOrChapterAdapter != null && juzOrChapterItemSelectListener != null) {
            setJuzOrChapterAdapter(juzOrChapterAdapter!!, juzOrChapterItemSelectListener!!, false)
        }

        mPopupBinding?.verseSec?.root?.disableView(false)
        mPopupBinding?.verseSec?.search?.disableView(false)
    }

    private fun setJuzOrChapterAdapter(
        adapter: ADPJuzChapterVerseBase<*, *>,
        listener: OnItemSelectedListener,
        isTemporary: Boolean = false
    ) {
        adapter.setSpinner(this)
        mPopupBinding?.juzChapterSec?.search?.text = null

        if (!isTemporary) {
            juzOrChapterAdapter = adapter
            juzOrChapterItemSelectListener = listener
        } else {
            tempJuzOrChapterItemSelectListener = listener
        }

        if (verseAdapter != null) visibility = VISIBLE

        mPopupBinding?.let {
            it.juzChapterSec.list.stopScroll()
            setupJuzOrChapterHeader(it, adapter)
            it.juzChapterSec.list.adapter = adapter
        }

        scrollToCurrentJuzOrChapter()
    }

    fun setVerseAdapter(adapter: VerseSelectorAdapter2, listener: (ReaderSpinnerItem) -> Unit) {
        adapter.setSpinner(this)

        verseAdapter = adapter
        verseItemSelectListener = listener

        if (juzOrChapterAdapter != null) visibility = VISIBLE

        mPopupBinding?.verseSec?.list?.adapter = adapter
    }

    fun prepareAndSetJuzAdapter(activity: ActivityReader, isTemporary: Boolean = false): JuzSelectorAdapter2 {
        val items = ArrayList<JuzSpinnerItem>()
        val juzNoText = context.getString(R.string.strLabelJuzNo)

        for (juzNo in 1..QuranMeta.totalJuzs()) {
            val item = JuzSpinnerItem(String.format(juzNoText, juzNo))
            item.juzNumber = juzNo
            items.add(item)
        }

        val juzAdapter = JuzSelectorAdapter2(items)

        setJuzOrChapterAdapter(
            juzAdapter,
            object : OnItemSelectedListener {
                override fun onItemSelect(item: ReaderSpinnerItem) {
                    val juzSpinnerItem = item as JuzSpinnerItem
                    if (activity.mNavigator.currJuzNo != juzSpinnerItem.juzNumber) {
                        activity.mNavigator.goToJuz(juzSpinnerItem.juzNumber)
                    }
                }
            },
            isTemporary
        )

        return juzAdapter
    }

    fun prepareAndSetChapterAdapter(activity: ActivityReader, isTemporary: Boolean = false): ChapterSelectorAdapter2 {
        val quran = activity.mQuranRef.get()
        val items = ArrayList<ChapterSpinnerItem>()
        for (chapterNo in 1..QuranMeta.totalChapters()) {
            val chapter = quran.getChapter(chapterNo)
            val chapterSpinnerItem = ChapterSpinnerItem(chapter)
            chapterSpinnerItem.label = chapter.name
            items.add(chapterSpinnerItem)
        }
        val chapterAdapter = ChapterSelectorAdapter2(items)
        setJuzOrChapterAdapter(
            chapterAdapter,
            object : OnItemSelectedListener {
                override fun onItemSelect(item: ReaderSpinnerItem) {
                    val chapterItem = item as ChapterSpinnerItem
                    if (activity.mReaderParams.currChapter?.chapterNumber != chapterItem.chapter.chapterNumber) {
                        activity.mNavigator.goToChapter(chapterItem.chapter.chapterNumber)
                    }
                }
            },
            isTemporary
        )

        return chapterAdapter
    }

    private fun findSelectedItemForText(adapter: ADPJuzChapterVerseBase<*, *>?): ReaderSpinnerItem? {
        if (adapter == null) return null

        var selectedItem = adapter.selectedItem
        if (selectedItem == null) {
            selectedItem = adapter.getItem(0)
        }

        return selectedItem
    }

    interface OnItemSelectedListener {
        fun onItemSelect(item: ReaderSpinnerItem)
    }
}
