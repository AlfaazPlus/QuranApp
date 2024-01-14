/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.AppBridge;
import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.api.ApiConfig;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.components.reader.ChapterVersePair;
import com.quranapp.android.databinding.LytReaderVodBinding;
import com.quranapp.android.databinding.LytReaderVodItemBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.services.RecitationService;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;

public class VerseOptionsDialog extends PeaceBottomSheet implements View.OnClickListener, BookmarkCallbacks {
    private ReaderPossessingActivity mActivity;
    private VODLayout mVODLayout;
    private LytReaderVodBinding mVODBinding;
    private Verse mVerse;
    @Nullable
    private BookmarkCallbacks mBookmarkCallbacks;
    private VerseShareDialog mVSD;
    private boolean mHasFootnotes;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ReaderPossessingActivity) {
            mActivity = (ReaderPossessingActivity) context;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable("verse", mVerse);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVerse = (Verse) savedInstanceState.getSerializable("verse");
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        if (mActivity != null && mVerse != null) {
            mActivity.getQuranMetaSafely(quranMeta -> setupDialogTitle(dialog.getContext(), quranMeta, mVerse));
        }
        super.setupDialog(dialog, style);
    }

    @Override
    protected void setupContentView(@NonNull LinearLayout dialogLayout, @NonNull PeaceBottomSheetParams params) {
        if (mActivity == null || mVerse == null) {
            return;
        }

        if (mVODBinding == null) {
            AsyncLayoutInflater inflater = new AsyncLayoutInflater(dialogLayout.getContext());
            inflater.inflate(R.layout.lyt_reader_vod, dialogLayout, (view, resid, parent) -> {
                mVODBinding = LytReaderVodBinding.bind(view);
                mVODLayout = new VODLayout(mVODBinding);

                setupContent(mActivity, mVODBinding, mVODLayout, dialogLayout, mVerse);
            });
        } else {
            setupContent(mActivity, mVODBinding, mVODLayout, dialogLayout, mVerse);
        }
    }

    private void setupContent(
        ReaderPossessingActivity actvt, LytReaderVodBinding vodBinding, VODLayout vodLayout,
        LinearLayout dialogLayout, Verse verse
    ) {
        ViewKt.removeView(vodBinding.getRoot());
        dialogLayout.addView(vodBinding.getRoot());

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) vodBinding.getRoot().getLayoutParams();
        lp.width = WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        vodBinding.getRoot().requestLayout();

        installContents(actvt, vodBinding, vodLayout, verse);

        if (!RecitationUtils.isRecitationSupported() || !(actvt instanceof ActivityReader)) {
            vodLayout.btnPlayControl.setVisibility(View.GONE);
        }
    }

    public void open(ReaderPossessingActivity actvt, Verse verse, @Nullable BookmarkCallbacks bookmarkCallbacks) {
        mActivity = actvt;
        mVerse = verse;
        mBookmarkCallbacks = bookmarkCallbacks;

        show(actvt.getSupportFragmentManager());
    }

    @Override
    public void dismiss() {
        try {
            dismissAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void installContents(ReaderPossessingActivity actvt, LytReaderVodBinding vodBinding, VODLayout vodLayout, Verse verse) {
        if (actvt instanceof ActivityReader) {
            ActivityReader reader = (ActivityReader) actvt;
            if (reader.mPlayerService != null) {
                onVerseRecite(reader.mPlayerService);
            }
        }


        boolean hasFootnotes = false;
        for (Translation translation : verse.getTranslations()) {
            if (translation.getFootnotesCount() > 0) {
                hasFootnotes = true;
                break;
            }
        }

        mHasFootnotes = hasFootnotes;
        disableButton(vodLayout.btnFootnotes, !hasFootnotes);

        final int chapterNo = verse.chapterNo;
        final int verseNo = verse.verseNo;
        onBookmarkChanged(actvt.isBookmarked(chapterNo, verseNo, verseNo));

        vodBinding.scrollView.scrollTo(0, 0);
    }

    private void setupDialogTitle(Context ctx, QuranMeta quranMeta, Verse verse) {
        String chapterName = quranMeta.getChapterName(ctx, mVerse.chapterNo);
        String title = ctx.getString(R.string.strTitleReaderVerseInformation, chapterName, verse.verseNo);
        getParams().setHeaderTitle(title);
        updateHeaderTitle();
    }

    private void onVerseRecite(RecitationService service) {
        final int chapterNo = service.getP().getCurrentChapterNo();
        final int verseNo = service.getP().getCurrentVerseNo();
        onVerseRecite(chapterNo, verseNo, service.isPlaying());
    }

    public void onVerseRecite(int chapterNo, int verseNo, boolean isReciting) {
        if (mVODLayout == null) {
            return;
        }

        if (mVODLayout.btnPlayControl.getVisibility() != View.VISIBLE) {
            return;
        }

        isReciting &= mVerse != null && mVerse.chapterNo == chapterNo && mVerse.verseNo == verseNo;

        int resId = isReciting ? R.drawable.dr_icon_pause_verse : R.drawable.dr_icon_play_verse;
        mVODLayout.btnPlayControlIcon.setImageResource(resId);

        int stringRes = isReciting ? R.string.strLabelPause : R.string.strLabelPlay;
        mVODLayout.btnPlayControlText.setText(stringRes);
    }

    private void onBookmarkChanged(boolean isBookmarked) {
        if (mVODLayout == null) {
            return;
        }

        Context ctx = mVODLayout.btnBookmarkIcon.getContext();

        final int filter = ContextCompat.getColor(ctx, isBookmarked ? R.color.colorPrimary : R.color.colorIcon2);
        mVODLayout.btnBookmarkIcon.setColorFilter(filter);

        final int res = isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined;
        mVODLayout.btnBookmarkIcon.setImageResource(res);

        int stringRes = isBookmarked ? R.string.strLabelBookmarked : R.string.strLabelBookmark;
        mVODLayout.btnBookmarkText.setText(stringRes);
    }

    private void disableButton(View btn, boolean disable) {
        btn.setAlpha(disable ? 0.5f : 1f);
    }

    private void initShareDialog(ReaderPossessingActivity actvt) {
        if (mVSD == null) {
            mVSD = new VerseShareDialog(actvt);
        }
    }

    public void openShareDialog(ReaderPossessingActivity actvt, int chapterNo, int verseNo) {
        initShareDialog(actvt);
        mVSD.show(chapterNo, verseNo);
    }

    public void dismissShareDialog() {
        if (mVSD == null) {
            return;
        }

        mVSD.hide();
    }

    @Override
    public void onClick(View v) {
        dismiss();
        ReaderPossessingActivity actvt = mActivity;
        if (actvt == null || mVerse == null) {
            return;
        }

        int id = v.getId();
        if (id == R.id.btnPlayControl) {
            if (actvt instanceof ActivityReader) {
                ActivityReader reader = (ActivityReader) actvt;
                if (reader.mPlayer != null) {
                    reader.mPlayer.reciteControl(new ChapterVersePair(mVerse.chapterNo, mVerse.verseNo));
                }
            }
        } else if (id == R.id.btnFootnotes) {
            if (mHasFootnotes) {
                actvt.showFootnotes(mVerse);
            } else {
                Toast.makeText(actvt, R.string.noFootnotesForThisVerse, Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.btnTafsir) {
            ReaderFactory.startTafsir(actvt, mVerse.chapterNo, mVerse.verseNo);
        } else if (id == R.id.btnBookmark) {
            final int chapterNo = mVerse.chapterNo;
            final int verseNo = mVerse.verseNo;

            boolean isBookmarked = actvt.isBookmarked(chapterNo, verseNo, verseNo);

            if (isBookmarked) {
                actvt.onBookmarkView(chapterNo, verseNo, verseNo, this);
            } else {
                actvt.addVerseToBookmark(chapterNo, verseNo, verseNo, this);
            }
        } else if (id == R.id.btnQuickEdit) {
            /* actvt.startQuickEditShare(mVerse);*/
        } else if (id == R.id.btnShare) {
            openShareDialog(actvt, mVerse.chapterNo, mVerse.verseNo);
        } else if (id == R.id.btnReport) {
            // redirect to github issues
            AppBridge.newOpener(actvt).browseLink(ApiConfig.GITHUB_ISSUES_VERSE_REPORT_URL);
        }
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        onBookmarkChanged(false);

        if (mBookmarkCallbacks != null) {
            mBookmarkCallbacks.onBookmarkRemoved(model);
        }
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        onBookmarkChanged(true);

        if (mBookmarkCallbacks != null) {
            mBookmarkCallbacks.onBookmarkAdded(model);
        }
    }

    @Override
    public void onBookmarkUpdated(BookmarkModel model) {
        if (mBookmarkCallbacks != null) {
            mBookmarkCallbacks.onBookmarkUpdated(model);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isShowing()) {
            mVODBinding = null;
        }
        if (mVSD != null) {
            mVSD.onLowMemory();
        }
    }

    private class VODLayout {
        private View btnPlayControl;
        private ImageView btnPlayControlIcon;
        private TextView btnPlayControlText;

        private View btnFootnotes;

        private ImageView btnBookmarkIcon;
        private TextView btnBookmarkText;

        public VODLayout(LytReaderVodBinding vodBinding) {
            init(vodBinding);
        }

        private void createLayout(LinearLayout options) {
            Context context = options.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            final TypedArray ids = context.getResources().obtainTypedArray(R.array.arrVODIds);
            final TypedArray icons = context.getResources().obtainTypedArray(R.array.arrVODIcons);
            final String[] labels = context.getResources().getStringArray(R.array.arrVODLabels);

            int marg = Dimen.dp2px(context, 10);
            int tint = ContextCompat.getColor(context, R.color.colorIcon);

            for (int i = 0, l = labels.length; i < l; i++) {
                int id = ids.getResourceId(i, -1);
                int icon = icons.getResourceId(i, 0);
                String label = labels[i];

                if (id == R.id.btnQuickEdit) {
                    continue;
                }

                LytReaderVodItemBinding binding = LytReaderVodItemBinding.inflate(inflater);
                binding.icon.setImageResource(icon);
                binding.label.setText(label);

                if (id != R.id.btnTafsir) {
                    binding.icon.setColorFilter(tint);
                }

                ViewGroup.MarginLayoutParams p = new ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                LayoutParamsKt.updateMargins(p, marg);

                binding.getRoot().setId(id);
                binding.getRoot().setOnClickListener(VerseOptionsDialog.this);
                options.addView(binding.getRoot(), p);
            }

            ids.recycle();
            icons.recycle();
        }

        private void collectViews(View container) {
            btnPlayControl = container.findViewById(R.id.btnPlayControl);
            btnPlayControlIcon = btnPlayControl.findViewById(R.id.icon);
            btnPlayControlText = btnPlayControl.findViewById(R.id.label);

            btnFootnotes = container.findViewById(R.id.btnFootnotes);

            View btnBookmark = container.findViewById(R.id.btnBookmark);
            btnBookmarkIcon = btnBookmark.findViewById(R.id.icon);
            btnBookmarkText = btnBookmark.findViewById(R.id.label);
        }

        public void init(LytReaderVodBinding vodBinding) {
            createLayout(vodBinding.options);
            collectViews(vodBinding.options);
        }
    }
}
