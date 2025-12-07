package com.quranapp.android.views.reader

import VerseItem
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.platform.ComposeView
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.ReaderPossessingActivity
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.composables.verse.VerseActionType
import com.quranapp.android.interfaceUtils.BookmarkCallbacks
import com.quranapp.android.utils.reader.factory.ReaderFactory

@SuppressLint("ViewConstructor")
class VerseViewComposable(
    private val mActivity: ReaderPossessingActivity,
    parent: ViewGroup?,
    verse: Verse?,
    private val mIsShowingAsReference: Boolean
) : FrameLayout(mActivity), BookmarkCallbacks {

    private val verseState = mutableStateOf<Verse?>(null, neverEqualPolicy())
    private val isBookmarkedState = mutableStateOf(false, neverEqualPolicy())
    private val isRecitingState = mutableStateOf(false, neverEqualPolicy())
    private val highlightColorState = mutableStateOf<Int?>(null, neverEqualPolicy())

    private val mHandler = Handler(Looper.getMainLooper())
    private val mResetHighlightRunnable = Runnable { highlightColorState.value = null }

    private val composeView = ComposeView(context)

    init {
        verseState.value = verse

        addView(composeView)

        setupComposeContent()

        if (verse != null) {
            updateBookmarkState(verse)
        }
    }

    private fun setupComposeContent() {
        composeView.setContent {
            val verse = verseState.value ?: return@setContent

            VerseItem(
                verse = verse,
                verseDecorator = mActivity.mVerseDecorator,
                isBookmarked = isBookmarkedState.value,
                isReciting = isRecitingState.value,
                isReference = mIsShowingAsReference,
                highlightColor = highlightColorState.value,
            ) { actionType ->
                handleAction(actionType, verse)
            }
        }
    }

    private fun handleAction(actionType: VerseActionType, verse: Verse) {
        when (actionType) {
            is VerseActionType.Options -> {
                mActivity.openVerseOptionDialog(verse, this)
            }

            is VerseActionType.Recite -> {
                if (mActivity is ActivityReader) {
                    mActivity.mPlayer?.reciteControl(
                        ChapterVersePair(verse.chapterNo, verse.verseNo)
                    )
                }
            }

            is VerseActionType.Tafsir -> {
                ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo)
            }

            is VerseActionType.BookmarkToggle -> {
                if (mActivity.isBookmarked(verse.chapterNo, verse.verseNo, verse.verseNo)) {
                    mActivity.onBookmarkView(
                        verse.chapterNo, verse.verseNo, verse.verseNo, this
                    )
                } else {
                    mActivity.addVerseToBookmark(
                        verse.chapterNo, verse.verseNo, verse.verseNo, this
                    )
                }
            }
        }
    }

    private fun updateBookmarkState(verse: Verse) {
        isBookmarkedState.value =
            mActivity.isBookmarked(verse.chapterNo, verse.verseNo, verse.verseNo)
    }

    /** Called by RecyclerView bind */
    fun setVerse(verse: Verse) {
        verseState.value = verse
        updateBookmarkState(verse)
    }

    fun getVerse(): Verse? = verseState.value

    fun highlightOnScroll() {
        highlightColorState.value = mActivity.mVerseHighlightedBGColor
        mHandler.removeCallbacks(mResetHighlightRunnable)
        mHandler.postDelayed(mResetHighlightRunnable, 1000)
    }

    fun onRecite(isReciting: Boolean) {
        isRecitingState.value = isReciting

        if (isReciting) {
            highlightColorState.value = mActivity.mVerseHighlightedBGColor
            mHandler.removeCallbacks(mResetHighlightRunnable)
        } else {
            highlightColorState.value = null
        }
    }

    override fun onBookmarkRemoved(model: BookmarkModel?) {
        isBookmarkedState.value = false
    }

    override fun onBookmarkAdded(model: BookmarkModel?) {
        isBookmarkedState.value = true
    }
}
