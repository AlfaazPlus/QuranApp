package com.quranapp.android.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.ADPBookmark;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.ActivityBookmarkBinding;
import com.quranapp.android.db.bookmark.BookmarkDBHelper;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.suppliments.BookmarkViewer;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.views.BoldHeader;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityBookmark extends BaseActivity implements BookmarkCallbacks {
    public final AtomicReference<QuranMeta> quranMetaRef = new AtomicReference<>();
    private final CallableTaskRunner<ArrayList<BookmarkModel>> taskRunner = new CallableTaskRunner<>();
    private ActivityBookmarkBinding mBinding;
    private BookmarkDBHelper mBookmarkDBHelper;
    private BookmarkViewer mBookmarkViewer;
    private ADPBookmark mAdapter;

    @Override
    protected void onDestroy() {
        if (mBookmarkDBHelper != null) {
            mBookmarkDBHelper.close();
        }
        if (mBookmarkViewer != null) {
            mBookmarkViewer.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBookmarkDBHelper != null && mAdapter != null) {
            refreshBookmarks();
        }
    }

    @Override
    public void onBackPressed() {
        if (mAdapter != null && mAdapter.mIsSelecting) {
            mAdapter.clearSelection();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_bookmark;
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        super.preActivityInflate(savedInstanceState);
        mBookmarkDBHelper = new BookmarkDBHelper(this);
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityBookmarkBinding.bind(activityView);

        /*mBookmarkDBHelper.addToBookmark(4, 5, 5, null, null);
        mBookmarkDBHelper.addToBookmark(6, 8, 8, null, null);
        mBookmarkDBHelper.addToBookmark(14, 15, 15, null, null);
        mBookmarkDBHelper.addToBookmark(33, 11, 12, null, null);
        mBookmarkDBHelper.addToBookmark(14, 5, 5, null, null);
        mBookmarkDBHelper.addToBookmark(16, 8, 8, null, null);
        mBookmarkDBHelper.addToBookmark(24, 15, 15, null, null);
        mBookmarkDBHelper.addToBookmark(34, 11, 12, null, null);*/

        initHeader(mBinding.header);

        QuranMeta.prepareInstance(this, quranMeta -> {
            quranMetaRef.set(quranMeta);
            init();
        });
    }

    private void init() {
        mBookmarkViewer = new BookmarkViewer(this, quranMetaRef, mBookmarkDBHelper, this);

        mBinding.noItemsIcon.setImageResource(R.drawable.dr_icon_bookmark_outlined);
        mBinding.noItemsText.setText(R.string.strMsgBookmarkNoItems);
        mBinding.loader.setVisibility(View.VISIBLE);
        refreshBookmarks();
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(new BoldHeader.BoldHeaderCallback() {
            @Override
            public void onBackIconClick() {
                getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onRightIconClick() {
                deleteAllWithCheckpoint();
            }
        });
        header.setTitleText(R.string.strTitleBookmarks);

        header.setLeftIconRes(R.drawable.dr_icon_arrow_left);
        header.setRightIconRes(R.drawable.dr_icon_delete);
        header.setShowRightIcon(true);

        header.setBGColor(R.color.colorBGPage);
    }

    private void deleteAllWithCheckpoint() {
        boolean isSelecting = mAdapter != null && mAdapter.mIsSelecting && !mAdapter.mSelectedModels.isEmpty();
        String title = isSelecting ? getString(R.string.strTitleBookmarkDeleteCount,
                mAdapter.mSelectedModels.size()) : getString(
                R.string.strTitleBookmarkDeleteAll);
        int dec = isSelecting ? R.string.strMsgBookmarkDeleteSelected : R.string.strMsgBookmarkDeleteAll;
        int labelNeg = isSelecting ? R.string.strLabelRemove : R.string.strLabelRemoveAll;

        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(title);
        builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setMessage(dec);
        builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setButtonsDirection(PeaceDialog.STACKED);
        builder.setDialogGravity(PeaceDialog.GRAVITY_BOTTOM);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setNegativeButton(labelNeg, ColorUtils.DANGER, (dialog, which) -> {
            if (isSelecting) {
                long[] ids = mAdapter.mSelectedModels.stream().mapToLong(BookmarkModel::getId).toArray();
                mBookmarkDBHelper.removeBookmarksBulk(ids, this::refreshBookmarks);
            } else {
                mBookmarkDBHelper.removeAllBookmarks();
                refreshBookmarks();
            }
        });
        builder.setFocusOnNegative(true);
        builder.show();
    }

    private void refreshBookmarks() {
        taskRunner.callAsync(new BookmarkFetcher());
    }

    private void setupAdapter(ArrayList<BookmarkModel> models) {
        if (mAdapter == null && models.size() > 0) {
            Context context = this;

            int spanCount = WindowUtils.isLandscapeMode(context) ? 2 : 1;
            GridLayoutManager layoutManager = new GridLayoutManager(context, spanCount);
            mBinding.list.setLayoutManager(layoutManager);

            mAdapter = new ADPBookmark(this, models);
            mBinding.list.setAdapter(mAdapter);

            RecyclerView.ItemAnimator animator = mBinding.list.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }
        } else if (mAdapter != null) {
            mAdapter.updateModels(models);
        }

        mBinding.noItems.setVisibility(models.size() == 0 ? View.VISIBLE : View.GONE);
    }

    public CharSequence prepareSubtitleTitle(int fromVerse, int toVerse) {
        return mBookmarkViewer.prepareSubtitleTitle(fromVerse, toVerse);
    }

    public void noSavedItems() {
        mBinding.noItems.setVisibility(View.VISIBLE);
    }

    public void onSelection(int size) {
        RecyclerView.ItemAnimator animator = mBinding.list.getItemAnimator();
        BoldHeader header = mBinding.header;

        if (size > 0) {
            header.setLeftIconRes(R.drawable.dr_icon_close);
            header.setTitleText(getString(R.string.strLabelSelectedCount, size));
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }
        } else {
            header.setLeftIconRes(R.drawable.dr_icon_arrow_left);
            header.setTitleText(R.string.strTitleBookmarks);
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(true);
            }
        }
    }

    public void removeVerseFromBookmark(BookmarkModel model, int position) {
        mBookmarkViewer.setLastViewedItemPosition(position);
        mBookmarkViewer.removeVerseFromBookmark(model);
    }

    public void onView(BookmarkModel model, int position) {
        mBookmarkViewer.setLastViewedItemPosition(position);
        mBookmarkViewer.view(model);
    }

    public void onOpen(BookmarkModel model) {
        Intent intent = ReaderFactory.prepareVerseRangeIntent(model.getChapterNo(), model.getFromVerseNo(),
                model.getToVerseNo());
        intent.setClass(this, ActivityReader.class);
        startActivity(intent);
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        mAdapter.removeItemFromAdapter(mBookmarkViewer.getLastViewedItemPosition());
        mBookmarkViewer.setLastViewedItemPosition(-1);
    }

    @Override
    public void onBookmarkUpdated(BookmarkModel model) {
        mAdapter.updateModel(model, mBookmarkViewer.getLastViewedItemPosition());
        mBookmarkViewer.setLastViewedItemPosition(-1);
    }

    private class BookmarkFetcher extends BaseCallableTask<ArrayList<BookmarkModel>> {
        @Override
        public ArrayList<BookmarkModel> call() {
            return mBookmarkDBHelper.getBookmarks();
        }

        @Override
        public void onComplete(@NonNull ArrayList<BookmarkModel> models) {
            setupAdapter(models);
        }

        @Override
        public void onFailed(@NonNull Exception e) {
            e.printStackTrace();
            Logger.reportError(e);
        }

        @Override
        public void postExecute() {
            mBinding.loader.setVisibility(View.GONE);
        }
    }
}