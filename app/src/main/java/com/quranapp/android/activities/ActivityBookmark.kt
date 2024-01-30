package com.quranapp.android.activities

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.utils.WindowUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.adapters.ADPBookmark
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.ActivityBookmarkBinding
import com.quranapp.android.db.bookmark.BookmarkDBHelper
import com.quranapp.android.interfaceUtils.BookmarkCallbacks
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.suppliments.BookmarkViewer
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareVerseRangeIntent
import com.quranapp.android.utils.thread.runner.CallableTaskRunner
import com.quranapp.android.utils.thread.tasks.BaseCallableTask
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import java.util.concurrent.atomic.AtomicReference

class ActivityBookmark : BaseActivity(), BookmarkCallbacks {
    val quranMetaRef = AtomicReference<QuranMeta>()
    private val taskRunner = CallableTaskRunner<ArrayList<BookmarkModel>>()
    private var mBinding: ActivityBookmarkBinding? = null
    private var mBookmarkDBHelper: BookmarkDBHelper? = null
    private var mBookmarkViewer: BookmarkViewer? = null
    private var mAdapter: ADPBookmark? = null
    override fun onDestroy() {
        if (mBookmarkDBHelper != null) {
            mBookmarkDBHelper!!.close()
        }
        if (mBookmarkViewer != null) {
            mBookmarkViewer!!.destroy()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (mBookmarkDBHelper != null && mAdapter != null) {
            refreshBookmarks()
        }
    }

    override fun onBackPressed() {
        if (mAdapter != null && mAdapter!!.mIsSelecting) {
            mAdapter!!.clearSelection()
        } else {
            super.onBackPressed()
        }
    }

    override fun getLayoutResource(): Int {
        return R.layout.activity_bookmark
    }

    override fun preActivityInflate(savedInstanceState: Bundle?) {
        super.preActivityInflate(savedInstanceState)
        mBookmarkDBHelper = BookmarkDBHelper(this)
    }

    override fun shouldInflateAsynchronously(): Boolean {
        return true
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        mBinding = ActivityBookmarkBinding.bind(activityView)

        /*mBookmarkDBHelper.addToBookmark(4, 5, 5, null, null);
        mBookmarkDBHelper.addToBookmark(6, 8, 8, null, null);
        mBookmarkDBHelper.addToBookmark(14, 15, 15, null, null);
        mBookmarkDBHelper.addToBookmark(33, 11, 12, null, null);
        mBookmarkDBHelper.addToBookmark(14, 5, 5, null, null);
        mBookmarkDBHelper.addToBookmark(16, 8, 8, null, null);
        mBookmarkDBHelper.addToBookmark(24, 15, 15, null, null);
        mBookmarkDBHelper.addToBookmark(34, 11, 12, null, null);*/

        initHeader(mBinding!!.header)

        QuranMeta.prepareInstance(
            this,
            object: OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    quranMetaRef.set(r)
                    init()
                }
            }
        )
    }

    private fun init() {
        mBookmarkViewer = BookmarkViewer(this, quranMetaRef, mBookmarkDBHelper, this)

        mBinding!!.noItemsIcon.setImageResource(R.drawable.dr_icon_bookmark_outlined)
        mBinding!!.noItemsText.setText(R.string.strMsgBookmarkNoItems)
        mBinding!!.loader.visibility = View.VISIBLE
        refreshBookmarks()
    }

    private fun initHeader(header: BoldHeader) {
        header.setCallback(object : BoldHeaderCallback {
            override fun onBackIconClick() {
                onBackPressedDispatcher.onBackPressed()
            }

            override fun onRightIconClick() {
                deleteAllWithCheckpoint()
            }
        })
        header.setTitleText(R.string.strTitleBookmarks)

        header.setLeftIconRes(R.drawable.dr_icon_arrow_left)
        header.setRightIconRes(R.drawable.dr_icon_delete)
        header.setShowRightIcon(true)

        header.setBGColor(R.color.colorBGPage)
    }

    private fun deleteAllWithCheckpoint() {
        val isSelecting = mAdapter != null && mAdapter!!.mIsSelecting && !mAdapter!!.mSelectedModels.isEmpty()
        val title = if (isSelecting) {
            getString(R.string.strTitleBookmarkDeleteCount, mAdapter!!.mSelectedModels.size)
        } else {
            getString(R.string.strTitleBookmarkDeleteAll)
        }
        val dec = if (isSelecting) R.string.strMsgBookmarkDeleteSelected else R.string.strMsgBookmarkDeleteAll
        val labelNeg = if (isSelecting) R.string.strLabelRemove else R.string.strLabelRemoveAll

        val builder = PeaceDialog.newBuilder(this)
        builder.setTitle(title)
        builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        builder.setMessage(dec)
        builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        builder.setButtonsDirection(PeaceDialog.STACKED)
        builder.setDialogGravity(PeaceDialog.GRAVITY_BOTTOM)
        builder.setNeutralButton(R.string.strLabelCancel, null)
        builder.setNegativeButton(labelNeg, ColorUtils.DANGER) { dialog: DialogInterface?, which: Int ->
            if (isSelecting) {
                val ids = mAdapter!!.mSelectedModels.stream().mapToLong(BookmarkModel::id).toArray()
                mBookmarkDBHelper!!.removeBookmarksBulk(ids) { refreshBookmarks() }
            } else {
                mBookmarkDBHelper!!.removeAllBookmarks()
                refreshBookmarks()
            }
        }
        builder.setFocusOnNegative(true)
        builder.show()
    }

    private fun refreshBookmarks() {
        taskRunner.callAsync(BookmarkFetcher())
    }

    private fun setupAdapter(models: ArrayList<BookmarkModel>) {
        if (mAdapter == null && models.size > 0) {
            val context: Context = this

            val spanCount = if (WindowUtils.isLandscapeMode(context)) 2 else 1
            val layoutManager = GridLayoutManager(context, spanCount)
            mBinding!!.list.layoutManager = layoutManager

            mAdapter = ADPBookmark(this, models)
            mBinding!!.list.adapter = mAdapter

            val animator = mBinding!!.list.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
        } else if (mAdapter != null) {
            mAdapter!!.updateModels(models)
        }

        mBinding!!.noItems.visibility = if (models.size == 0) View.VISIBLE else View.GONE
    }

    fun prepareSubtitleTitle(fromVerse: Int, toVerse: Int): CharSequence {
        return mBookmarkViewer!!.prepareSubtitleTitle(fromVerse, toVerse)
    }

    fun noSavedItems() {
        mBinding!!.noItems.visibility = View.VISIBLE
    }

    fun onSelection(size: Int) {
        val animator = mBinding!!.list.itemAnimator
        val header = mBinding!!.header

        if (size > 0) {
            header.setLeftIconRes(R.drawable.dr_icon_close)
            header.setTitleText(getString(R.string.strLabelSelectedCount, size))
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
        } else {
            header.setLeftIconRes(R.drawable.dr_icon_arrow_left)
            header.setTitleText(R.string.strTitleBookmarks)
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = true
            }
        }
    }

    fun removeVerseFromBookmark(model: BookmarkModel?, position: Int) {
        mBookmarkViewer!!.lastViewedItemPosition = position
        mBookmarkViewer!!.removeVerseFromBookmark(model)
    }

    fun onView(model: BookmarkModel?, position: Int) {
        mBookmarkViewer!!.lastViewedItemPosition = position
        mBookmarkViewer!!.view(model)
    }

    fun onOpen(model: BookmarkModel) {
        val intent = prepareVerseRangeIntent(model.chapterNo, model.fromVerseNo, model.toVerseNo)
        intent.setClass(this, ActivityReader::class.java)
        startActivity(intent)
    }

    override fun onBookmarkRemoved(model: BookmarkModel) {
        mAdapter!!.removeItemFromAdapter(mBookmarkViewer!!.lastViewedItemPosition)
        mBookmarkViewer!!.lastViewedItemPosition = -1
    }

    override fun onBookmarkUpdated(model: BookmarkModel) {
        mAdapter!!.updateModel(model, mBookmarkViewer!!.lastViewedItemPosition)
        mBookmarkViewer!!.lastViewedItemPosition = -1
    }

    inner class BookmarkFetcher : BaseCallableTask<ArrayList<BookmarkModel>?>() {
        override fun call(): ArrayList<BookmarkModel>? {
            return mBookmarkDBHelper!!.bookmarks
        }

        override fun onComplete(models: ArrayList<BookmarkModel>?) {
            setupAdapter(models!!)
        }

        override fun onFailed(e: Exception) {
            e.printStackTrace()
            Logger.reportError(e)
        }

        override fun postExecute() {
            mBinding!!.loader.visibility = View.GONE
        }
    }
}