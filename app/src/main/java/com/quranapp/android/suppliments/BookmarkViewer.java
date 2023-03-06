/*
 * (c) Faisal Khan. Created on 7/10/2021.
 */

package com.quranapp.android.suppliments;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.LytBookmarkDialogBinding;
import com.quranapp.android.db.bookmark.BookmarkDBHelper;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;

import java.util.concurrent.atomic.AtomicReference;

public class BookmarkViewer implements Destroyable {
    private final Context mContext;
    private final AtomicReference<QuranMeta> mQuranMetaRef;
    private final BookmarkDBHelper mDBHelper;
    private final boolean mIsReader;
    private final BookmarkCallbacks mBookmarkCallbacks;

    private PeaceDialog mPopup;
    private final String mVerseNoFormat;
    private final String mVersesFormat;
    private LytBookmarkDialogBinding mBinding;
    private BookmarkModel mInitialModel;
    private BookmarkModel mModel;

    // For ActivityBookmark only
    private int mLastViewItemPosition;
    private boolean mViewerOpenedForEdit;

    public BookmarkViewer(Context context, AtomicReference<QuranMeta> quranMetaRef, BookmarkDBHelper dbHelper, BookmarkCallbacks callbacks) {
        mContext = context;
        mQuranMetaRef = quranMetaRef;
        mDBHelper = dbHelper;
        mIsReader = context instanceof ActivityReader;
        mBookmarkCallbacks = callbacks;

        mVerseNoFormat = mContext.getString(R.string.strLabelVerseNoWithColon);
        mVersesFormat = mContext.getString(R.string.strLabelVersesWithColon);
    }

    private void createPopup(Context ctx, View view) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx, R.style.PeaceDialogThemeFullscreen);
        builder.setFullscreen(true);
        builder.setOnDismissListener(dialog -> {
            if (mModel != null) {
                saveEdit(mBinding, mModel);
            }

            toggleEdit(false, mBinding);
            mInitialModel = null;
            mModel = null;
        });
        mPopup = builder.create();
        mPopup.show();

        ViewKt.removeView(view);
        mPopup.setContentView(view);
    }

    private void init(Context ctx, Runnable runnable) {
        AsyncLayoutInflater inflater = new AsyncLayoutInflater(ctx);
        inflater.inflate(R.layout.lyt_bookmark_dialog, null, (view, resid, parent) -> {
            mBinding = LytBookmarkDialogBinding.bind(view);
            setup(mBinding);

            if (runnable != null) {
                runnable.run();
            }
        });
    }

    private void setup(LytBookmarkDialogBinding binding) {
        binding.noteEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                super.onTextChanged(s, start, before, count);
                binding.noteText.setText(s);
            }
        });
        binding.noteEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (mModel == null) {
                return false;
            }

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveEdit(binding, mModel);
            }
            return true;
        });

        for (View button : new View[]{binding.edit, binding.delete, binding.done, binding.openInReader}) {
            button.setOnTouchListener(new HoverPushOpacityEffect());
        }

        binding.edit.setOnClickListener(v -> editBookmark(binding));
        binding.delete.setOnClickListener(v -> {
            if (mModel != null) {
                removeVerseFromBookmark(mModel);
            }
        });

        binding.done.setOnClickListener(v -> {
            if (mModel != null) {
                saveEdit(binding, mModel);
            }

            if (mViewerOpenedForEdit) {
                close();
            }
        });
        binding.openInReader.setOnClickListener(v -> {
            if (mModel != null) {
                open(mModel);
            }

            close();
        });

        binding.close.setOnClickListener(v -> close());

        binding.openInReader.setVisibility(mIsReader ? View.GONE : View.VISIBLE);
        binding.openInReader.setEnabled(!mIsReader);
    }

    private void setupOnView(BookmarkModel model, LytBookmarkDialogBinding binding) {
        createPopup(mContext, binding.getRoot());

        toggleEdit(false, binding);

        QuranMeta quranMeta = mQuranMetaRef.get();
        if (quranMeta != null) {
            binding.chapterTitle.setText(quranMeta.getChapterName(mContext, model.getChapterNo(), true));
        }
        binding.subtitle.setText(prepareSubtitleTitle(model.getFromVerseNo(), model.getToVerseNo()));
        binding.noteText.setText(model.getNote());
        binding.noteEditText.setText(model.getNote());
    }

    public CharSequence prepareSubtitleTitle(int fromVerse, int toVerse) {
        if (QuranUtils.doesRangeDenoteSingle(fromVerse, toVerse)) {
            return String.format(mVerseNoFormat, fromVerse);
        } else {
            return String.format(mVersesFormat, fromVerse, toVerse);
        }
    }

    private void editBookmark(LytBookmarkDialogBinding binding) {
        toggleEdit(true, binding);
    }

    private void toggleEdit(boolean editing, LytBookmarkDialogBinding binding) {
        int visibility = editing ? View.GONE : View.VISIBLE;
        int visibilityInverse = editing ? View.VISIBLE : View.GONE;

        binding.edit.setVisibility(visibility);
        binding.delete.setVisibility(visibility);
        binding.noteText.setVisibility(visibility);
        binding.openInReader.setVisibility(!mIsReader && !editing ? View.VISIBLE : View.GONE);

        binding.done.setVisibility(visibilityInverse);
        binding.noteEditText.setVisibility(visibilityInverse);
        binding.addNoteTitle.setVisibility(visibilityInverse);

        binding.noteEditText.postDelayed(() -> {
            InputMethodManager imm = ContextCompat.getSystemService(mContext, InputMethodManager.class);
            if (imm != null) {
                if (editing) {
                    binding.noteEditText.requestFocus();
                    imm.showSoftInput(binding.noteEditText, InputMethodManager.SHOW_IMPLICIT);

                    Editable text = binding.noteEditText.getText();
                    if (text != null) {
                        binding.noteEditText.setSelection(text.length());
                    }
                } else {
                    binding.noteEditText.clearFocus();
                    imm.hideSoftInputFromWindow(binding.noteEditText.getWindowToken(), 0);
                }
            }
        }, 100);
    }

    private void saveEdit(LytBookmarkDialogBinding binding, BookmarkModel model) {
        Editable text = binding.noteEditText.getText();

        binding.noteText.setText(text);

        model.setNote(text != null && text.length() > 0 ? text.toString() : null);
        toggleEdit(false, binding);

        if (!model.equals(mInitialModel)) {
            mDBHelper.updateBookmark(model.getChapterNo(), model.getFromVerseNo(), model.getToVerseNo(),
                model.getNote(), nModel -> {
                    if (mBookmarkCallbacks != null) {
                        mBookmarkCallbacks.onBookmarkUpdated(nModel);
                    }
                });
        }
    }

    public void open(BookmarkModel model) {
        Intent intent = ReaderFactory.prepareVerseRangeIntent(model.getChapterNo(), model.getFromVerseNo(),
            model.getToVerseNo());
        intent.setClass(mContext, ActivityReader.class);
        mContext.startActivity(intent);
    }

    public void removeVerseFromBookmark(BookmarkModel model) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(mContext);
        builder.setTitle(R.string.strTitleBookmarkDeleteThis);
        builder.setMessage(" ");
        builder.setButtonsDirection(PeaceDialog.STACKED);
        builder.setDialogGravity(PeaceDialog.GRAVITY_BOTTOM);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setNegativeButton(R.string.strLabelRemove, ColorUtils.DANGER,
            (dialog, which) -> removeBookmarkFinal(model));
        builder.setFocusOnNegative(true);
        builder.show();
    }

    private void removeBookmarkFinal(BookmarkModel model) {
        mDBHelper.removeFromBookmark(model.getChapterNo(), model.getFromVerseNo(), model.getToVerseNo(), () -> {
            if (mBookmarkCallbacks != null) {
                mBookmarkCallbacks.onBookmarkRemoved(model);
            }
            close();
        });
    }

    public void view(int chapterNo, int fromVerse, int toVerse) {
        try {
            BookmarkModel bookmark = mDBHelper.getBookmark(chapterNo, fromVerse, toVerse);
            if (bookmark != null) {
                view(bookmark);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.reportError(e);
        }
    }

    public void view(BookmarkModel model) {
        mInitialModel = model.copy();
        mModel = model;

        if (mBinding == null) {
            init(mContext, () -> setupOnView(model, mBinding));
        } else {
            setupOnView(model, mBinding);
        }
    }

    public void edit(BookmarkModel model) {
        mModel = model;
        mViewerOpenedForEdit = true;

        if (mBinding == null) {
            init(mContext, () -> {
                setupOnView(model, mBinding);
                editBookmark(mBinding);
            });
        } else {
            setupOnView(model, mBinding);
            editBookmark(mBinding);
        }
    }

    private void close() {
        mViewerOpenedForEdit = false;
        if (mPopup != null) {
            mPopup.dismiss();
        }
    }

    public void setLastViewedItemPosition(int position) {
        mLastViewItemPosition = position;
    }

    public int getLastViewedItemPosition() {
        return mLastViewItemPosition;
    }

    public void setQuranMeta(QuranMeta quranMeta) {
        mQuranMetaRef.set(quranMeta);
    }

    @Override
    public void destroy() {
        if (mDBHelper != null) {
            mDBHelper.close();
        }
        close();
    }
}
