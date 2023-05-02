/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */
package com.quranapp.android.views.reader.dialogs

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.databinding.LytPageVerseDialogBinding
import com.quranapp.android.databinding.LytReaderVerseQuickActionsBinding
import com.quranapp.android.interfaceUtils.BookmarkCallbacks
import com.quranapp.android.utils.extensions.layoutInflater
import com.quranapp.android.utils.extensions.removeView
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.reader.factory.ReaderFactory.startTafsir
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.SelectableLinkMovementMethod
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams

class PageVerseDialog : PeaceBottomSheet() {
    private var binding: LytPageVerseDialogBinding? = null
    private var reader: ActivityReader? = null
    private var verse: Verse? = null

    init {
        params.apply {
            initialBehaviorState = BottomSheetBehavior.STATE_EXPANDED
            headerShown = false
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActivityReader) {
            reader = context
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("verse", verse)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            verse = savedInstanceState.serializableExtra("verse")
        }
        super.onCreate(savedInstanceState)
    }


    override fun setupContentView(dialogLayout: LinearLayout, params: PeaceBottomSheetParams) {
        if (reader == null || verse == null) return

        if (binding != null) {
            binding!!.let {
                it.root.removeView()
                dialogLayout.addView(it.root)
                setupContent(it, verse!!, reader!!)
            }
        } else {
            AsyncLayoutInflater(dialogLayout.context).inflate(
                R.layout.lyt_page_verse_dialog,
                dialogLayout
            ) { view: View, _, _ ->
                dialogLayout.addView(view)
                binding = LytPageVerseDialogBinding.bind(view)
                setupContent(binding!!, verse!!, reader!!)
            }
        }
    }

    private fun setupContent(binding: LytPageVerseDialogBinding, verse: Verse, activity: ActivityReader) {
        onDismissListener = object : OnPeaceBottomSheetDismissListener {
            override fun onDismissed() {
                this@PageVerseDialog.binding = null
                this@PageVerseDialog.reader = null
                this@PageVerseDialog.verse = null
            }
        }

        activity.mPlayerService?.let {
            onVerseRecite(it.p.currentChapterNo, it.p.currentVerseNo, it.isPlaying)
        }

        verse.apply {
            val translSlugs = SPReader.getSavedTranslations(reader)
            val booksInfo = activity.mTranslFactory.getTranslationBooksInfoValidated(translSlugs)
            val transl = activity.mTranslFactory.getTranslationsSingleVerse(
                translSlugs,
                verse.chapterNo,
                verse.verseNo
            )

            translations = transl
            translTextSpannable = activity.prepareTranslSpannable(verse, translations, booksInfo)
        }

        binding.root.isSaveEnabled = true
        binding.translations.setShadowLayer(20f, 0f, 0f, Color.TRANSPARENT)
        binding.translations.text = verse.translTextSpannable
        binding.translations.movementMethod = SelectableLinkMovementMethod.getInstance()

        activity.mVerseDecorator.let {
            it.setTextColorNonArabic(binding.translations)
            it.setTextSizeTransl(binding.translations)
        }

        val bookmarkCallbacks = object : BookmarkCallbacks {
            override fun onBookmarkRemoved(model: BookmarkModel?) {
                onBookmarkChanged(binding.quickActions, false)
            }

            override fun onBookmarkAdded(model: BookmarkModel?) {
                onBookmarkChanged(binding.quickActions, true)
            }
        }

        binding.quickActions.let {
            val chapterNo = verse.chapterNo
            val verseNo = verse.verseNo

            val name = activity.mQuranMetaRef.get()!!.getChapterName(activity, chapterNo)

            it.verseSerial.contentDescription = activity.getString(R.string.strDescVerseNoWithChapter, name, verseNo)
            it.verseSerial.text = activity.getString(R.string.strLabelVerseSerialWithChapter, name, chapterNo, verseNo)

            it.btnVerseRecitation.visibility = if (!RecitationUtils.isRecitationSupported()) View.GONE else View.VISIBLE

            it.btnVerseOptions.setOnClickListener {
                dismiss()
                activity.openVerseOptionDialog(verse, bookmarkCallbacks)
            }

            it.btnVerseRecitation.setOnClickListener {
                activity.mPlayer?.reciteControl(ChapterVersePair(chapterNo, verseNo))
            }

            it.btnTafsir.setOnClickListener { v ->
                dismiss()
                startTafsir(v.context, chapterNo, verseNo)
            }
            ViewCompat.setTooltipText(it.btnTafsir, activity.getString(R.string.strTitleTafsir))

            onBookmarkChanged(it, activity.isBookmarked(chapterNo, verseNo, verseNo))
            it.btnBookmark.setOnClickListener {
                if (activity.isBookmarked(chapterNo, verseNo, verseNo)) {
                    activity.onBookmarkView(chapterNo, verseNo, verseNo, bookmarkCallbacks)
                } else {
                    activity.addVerseToBookmark(chapterNo, verseNo, verseNo, bookmarkCallbacks)
                }
            }
            ViewCompat.setTooltipText(it.btnBookmark, activity.getString(R.string.strLabelBookmark))
        }
    }

    private fun onBookmarkChanged(binding: LytReaderVerseQuickActionsBinding, isBookmarked: Boolean) {
        binding.btnBookmark.setColorFilter(
            ContextCompat.getColor(
                binding.root.context,
                if (isBookmarked) R.color.colorPrimary else R.color.colorIcon2
            )
        )
        binding.btnBookmark.setImageResource(
            if (isBookmarked) R.drawable.dr_icon_bookmark_added
            else R.drawable.dr_icon_bookmark_outlined
        )
    }

    fun onVerseRecite(chapterNo: Int, verseNo: Int, isReciting: Boolean) {
        if (verse == null || binding == null) return

        val reciting = isReciting and (verse!!.chapterNo == chapterNo && verse!!.verseNo == verseNo)
        val resId = if (reciting) R.drawable.dr_icon_pause2 else R.drawable.dr_icon_play2
        binding!!.quickActions.btnVerseRecitation.setImageResource(resId)
    }

    fun show(reader: ActivityReader, verse: Verse) {
        this.reader = reader
        this.verse = verse.copy()

        show(reader.supportFragmentManager)
    }

    override fun dismiss() {
        try {
            dismissAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}