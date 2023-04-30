package com.quranapp.android.views.reader

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.DrawableUtils
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.LytReaderHeaderBinding
import com.quranapp.android.interfaceUtils.Destroyable
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.utils.univ.Keys.READER_KEY_READ_TYPE
import com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES
import com.quranapp.android.utils.univ.Keys.READER_KEY_SETTING_IS_FROM_READER
import com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem
import com.quranapp.android.views.reader.verseSpinner.VerseSpinnerItem
import com.quranapp.android.views.readerSpinner2.adapters.ChapterSelectorAdapter2
import com.quranapp.android.views.readerSpinner2.adapters.JuzSelectorAdapter2
import com.quranapp.android.views.readerSpinner2.adapters.VerseSelectorAdapter2

class ReaderHeader @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppBarLayout(context, attrs, defStyleAttr), Destroyable {
    val binding = LytReaderHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    private var activity: ActivityReader? = null

    private val jcvSelectorView = binding.readerTitle.juzChapterVerseSpinner
    private var chapterAdapter: ChapterSelectorAdapter2? = null
    private var verseAdapter: VerseSelectorAdapter2? = null
    private var juzAdapter: JuzSelectorAdapter2? = null
    private var readerParams: ReaderParams? = null

    init {
        background = DrawableUtils.createBackgroundStroked(
            ContextCompat.getColor(context, R.color.colorBGReaderHeader),
            ContextCompat.getColor(context, R.color.colorDivider),
            Dimen.createBorderWidthsForBG(0, 0, 0, Dimen.dp2px(context, 1f)),
            null
        )

        (binding.root.layoutParams as? LayoutParams)?.let {
            it.scrollFlags = LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or LayoutParams.SCROLL_FLAG_SNAP
            binding.root.layoutParams = it
        }


        binding.back.setOnClickListener {
            activity?.let {
                it.finish()

                if (it.isTaskRoot) {
                    it.launchMainActivity()
                }
            }
        }

        binding.readerSetting.let {
            it.setOnClickListener { openReaderSetting(-1) }
            ViewCompat.setTooltipText(it, context.getString(R.string.strTitleReaderSettings))
        }

        binding.btnTranslLauncher.let {
            it.setOnClickListener { openReaderSetting(ActivitySettings.SETTINGS_TRANSLATION) }
            ViewCompat.setTooltipText(it, context.getString(R.string.strLabelSelectTranslations))
        }

        binding.readerTitle.let {
            it.root.setOnClickListener { jcvSelectorView.showPopup() }
            jcvSelectorView.juzIconView = it.juzIcon
            jcvSelectorView.chapterIconView = it.chapterIcon
        }
    }

    fun openReaderSetting(destination: Int) {
        if (readerParams == null) return

        activity?.startActivity4Result(
            Intent(activity, ActivitySettings::class.java).apply {
                putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, destination)
                putExtra(READER_KEY_SETTING_IS_FROM_READER, true)
                putExtra(READER_KEY_SAVE_TRANSL_CHANGES, readerParams!!.saveTranslChanges)
                if (readerParams!!.visibleTranslSlugs != null) {
                    putExtra(READER_KEY_TRANSL_SLUGS, readerParams!!.visibleTranslSlugs!!.toTypedArray())
                }
                putExtra(READER_KEY_READ_TYPE, readerParams!!.readType)
            },
            null
        )
    }

    fun initJuzSelector() {
        if (activity == null || readerParams == null) return

        if (readerParams!!.readType != ReaderParams.READER_READ_TYPE_JUZ || jcvSelectorView.juzOrChapterAdapter !is JuzSelectorAdapter2) {
            juzAdapter = jcvSelectorView.prepareAndSetJuzAdapter(activity!!, false)
        }
    }

    fun initChapterSelector() {
        if (activity == null || readerParams == null) {
            return
        }

        if (readerParams!!.readType == ReaderParams.READER_READ_TYPE_JUZ || jcvSelectorView.juzOrChapterAdapter !is ChapterSelectorAdapter2) {
            chapterAdapter = jcvSelectorView.prepareAndSetChapterAdapter(activity!!, false)
        }
    }

    fun initVerseSelector(adapter: VerseSelectorAdapter2?, chapterNo: Int) {
        if (activity == null) return

        var adp = adapter

        if (adp == null) {
            val verseNoText = context.getString(R.string.strLabelVerseNo)
            val items = ArrayList<VerseSpinnerItem>()
            var verseNo = 1
            val count = activity!!.mQuranMetaRef.get()!!.getChapterVerseCount(chapterNo)

            while (verseNo <= count) {
                items.add(VerseSpinnerItem(chapterNo, verseNo).apply {
                    label = String.format(verseNoText, verseNo)
                })
                verseNo++
            }

            adp = VerseSelectorAdapter2(items)
        }

        verseAdapter = adp

        jcvSelectorView.setVerseAdapter(adp) { item: ReaderSpinnerItem ->
            val verseSpinnerItem = item as VerseSpinnerItem
            activity?.handleVerseSpinnerSelectedVerseNo(verseSpinnerItem.chapterNo, verseSpinnerItem.verseNo)
        }
    }

    fun setupHeaderForReadType() {
        if (readerParams == null) return

        binding.readerTitle.let {
            val isJuz = readerParams!!.readType == ReaderParams.READER_READ_TYPE_JUZ
            it.chapterIcon.visibility = if (isJuz) GONE else VISIBLE
            it.juzIcon.visibility = if (isJuz) VISIBLE else GONE
            it.juzChapterVerseSpinner.visibility = VISIBLE
        }
    }

    fun setActivity(activity: ActivityReader) {
        this.activity = activity
        readerParams = activity.mReaderParams
        jcvSelectorView.activity = activity
    }

    fun selectJuzIntoSpinner(juzNo: Int) {
        if (readerParams == null) return

        readerParams!!.currJuzNo = juzNo

        juzAdapter?.let {
            val selectedItem = it.selectedItem
            if (it.selectedItem != null && selectedItem!!.juzNumber == juzNo) return

            for (i in 0 until it.itemCount) {
                if (it.getItem(i).juzNumber == juzNo) {
                    it.selectSansInvocation(i)
                    break
                }
            }
        }
    }

    fun selectChapterIntoSpinner(chapterNo: Int) {
        chapterAdapter?.let {
            val selectedItem = it.selectedItem
            if (selectedItem != null && selectedItem.chapter.chapterNumber == chapterNo) return

            for (i in 0 until it.itemCount) {
                if (it.getItem(i).chapter.chapterNumber == chapterNo) {
                    it.selectSansInvocation(i)
                    break
                }
            }
        }
    }

    fun selectVerseIntoSpinner(chapterNo: Int, verseNo: Int) {
        readerParams?.currChapter?.let {
            it.currentVerseNo = verseNo
        }

        verseAdapter?.let {
            val selectedItem = it.selectedItem
            if (selectedItem != null && selectedItem.chapterNo == chapterNo && selectedItem.verseNo == verseNo) return

            for (i in 0 until it.itemCount) {
                val item = it.getItem(i)
                if (item.chapterNo == chapterNo && item.verseNo == verseNo) {
                    it.selectSansInvocation(i)
                    break
                }
            }
        }
    }

    override fun destroy() {
        jcvSelectorView.closePopup()
    }
}