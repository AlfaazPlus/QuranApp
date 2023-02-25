package com.quranapp.android.views.reader;

import static com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS;
import static com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL;
import static com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_READ_TYPE;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SETTING_IS_FROM_READER;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.databinding.LytReaderHeaderBinding;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.reader_managers.Navigator;
import com.quranapp.android.reader_managers.ReaderParams;
import com.quranapp.android.views.reader.chapterSpinner.ChapterSpinnerItem;
import com.quranapp.android.views.reader.juzSpinner.JuzSpinnerItem;
import com.quranapp.android.views.reader.verseSpinner.VerseSpinnerItem;
import com.quranapp.android.views.readerSpinner2.adapters.ChapterSelectorAdapter2;
import com.quranapp.android.views.readerSpinner2.adapters.JuzSelectorAdapter2;
import com.quranapp.android.views.readerSpinner2.adapters.VerseSelectorAdapter2;
import com.quranapp.android.views.readerSpinner2.juzChapterVerse.JuzChapterVerseSelector;

import java.util.ArrayList;

public class ReaderHeader extends AppBarLayout implements Destroyable {
    public final LytReaderHeaderBinding mBinding;
    private ActivityReader mActivity;
    // public final JuzSpinner mJuzSpinner;
    // public final ChapterSpinner mChapterSpinner;
    // public final VerseSpinner mVerseSpinner;
    private final JuzChapterVerseSelector mJCVSelectorView;
    private ChapterSelectorAdapter2 mChapterAdapter;
    private VerseSelectorAdapter2 mVerseAdapter;
    private JuzSelectorAdapter2 mJuzAdapter;
    private ReaderParams mReaderParams;

    public ReaderHeader(@NonNull Context context) {
        this(context, null);
    }

    public ReaderHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReaderHeader(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBinding = LytReaderHeaderBinding.inflate(LayoutInflater.from(context), this, false);
        mJCVSelectorView = mBinding.readerTitle.juzChapterVerseSpinner;
        addView(mBinding.getRoot());
        init();
    }

    private void init() {
        initThis();

        mBinding.back.setOnClickListener(v -> {
            mActivity.finish();
            if (mActivity.isTaskRoot()) {
                mActivity.launchMainActivity();
            }
        });
        mBinding.readerSetting.setOnClickListener(v -> openReaderSetting(-1));
        mBinding.btnTranslLauncher.setOnClickListener(v -> openReaderSetting(ActivitySettings.SETTINGS_TRANSL));

        ViewCompat.setTooltipText(mBinding.readerSetting, getContext().getString(R.string.strTitleReaderSettings));
        ViewCompat.setTooltipText(mBinding.btnTranslLauncher, getContext().getString(R.string.strLabelSelectTranslations));

        mBinding.readerTitle.getRoot().setOnClickListener(v -> mJCVSelectorView.showPopup());
        mJCVSelectorView.setJuzIconView(mBinding.readerTitle.juzIcon);
        mJCVSelectorView.setChapterIconView(mBinding.readerTitle.chapterIcon);
    }

    public void openReaderSetting(int destination) {
        Intent intent = new Intent(mActivity, ActivitySettings.class);
        intent.putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, destination);
        intent.putExtra(READER_KEY_SETTING_IS_FROM_READER, true);
        intent.putExtra(READER_KEY_SAVE_TRANSL_CHANGES, mReaderParams.saveTranslChanges);
        if (mReaderParams.getVisibleTranslSlugs() != null) {
            intent.putExtra(READER_KEY_TRANSL_SLUGS, mReaderParams.getVisibleTranslSlugs().toArray(new String[0]));
        }
        intent.putExtra(READER_KEY_READ_TYPE, mReaderParams.readType);
        mActivity.startActivity4Result(intent, null);
    }

    private void initThis() {
        setBackground(DrawableUtils.createBackgroundStroked(
                ContextCompat.getColor(getContext(), R.color.colorBGReaderHeader),
                ContextCompat.getColor(getContext(), R.color.colorDivider),
                Dimen.createBorderWidthsForBG(0, 0, 0, Dimen.dp2px(getContext(), 1)),
                null
        ));

        ViewGroup.LayoutParams params = mBinding.getRoot().getLayoutParams();
        if (params instanceof AppBarLayout.LayoutParams) {
            ((LayoutParams) params).setScrollFlags(SCROLL_FLAG_SCROLL | SCROLL_FLAG_ENTER_ALWAYS | SCROLL_FLAG_SNAP);
            mBinding.getRoot().setLayoutParams(params);
        }
    }


    public void initJuzSelector() {
        if (mActivity == null) {
            return;
        }
        if (mReaderParams != null && mReaderParams.readType == READER_READ_TYPE_JUZ
                && mJCVSelectorView.getJuzOrChapterAdapter() instanceof JuzSelectorAdapter2) {
            return;
        }

        mJuzAdapter = mJCVSelectorView.prepareAndSetJuzAdapter(mActivity, false);
    }

    public void initChapterSelector() {
        if (mActivity == null) {
            return;
        }

        if (mReaderParams != null && mReaderParams.readType != READER_READ_TYPE_JUZ
                && mJCVSelectorView.getJuzOrChapterAdapter() instanceof ChapterSelectorAdapter2) {
            return;
        }

        mChapterAdapter = mJCVSelectorView.prepareAndSetChapterAdapter(mActivity, false);
    }

    public void initVerseSelector(VerseSelectorAdapter2 adapter, int chapterNo) {
        if (mActivity == null) {
            return;
        }

        if (adapter == null) {
            String verseNoText = getContext().getString(R.string.strLabelVerseNo);
            ArrayList<VerseSpinnerItem> items = new ArrayList<>();
            for (int verseNo = 1, l = mActivity.mQuranMetaRef.get().getChapterVerseCount(chapterNo); verseNo <= l; verseNo++) {
                VerseSpinnerItem item = new VerseSpinnerItem(chapterNo, verseNo);
                item.setLabel(String.format(verseNoText, verseNo));
                items.add(item);
            }
            adapter = new VerseSelectorAdapter2(items);
        }

        mVerseAdapter = adapter;
        mJCVSelectorView.setVerseAdapter(adapter, item -> {
            VerseSpinnerItem verseSpinnerItem = (VerseSpinnerItem) item;
            mActivity.handleVerseSpinnerSelectedVerseNo(verseSpinnerItem.getChapterNo(), verseSpinnerItem.getVerseNo());
        });
    }

    public void setupHeaderForReadType() {
        if (mReaderParams.readType == READER_READ_TYPE_JUZ) {
            mBinding.readerTitle.chapterIcon.setVisibility(GONE);
            mBinding.readerTitle.juzIcon.setVisibility(VISIBLE);

            // mBinding.readerTitle.chapterSpinner.setVisibility(GONE);
            // mBinding.readerTitle.juzSpinner.setVisibility(VISIBLE);
        } else {
            mBinding.readerTitle.chapterIcon.setVisibility(VISIBLE);
            mBinding.readerTitle.juzIcon.setVisibility(GONE);

            // mBinding.readerTitle.chapterSpinner.setVisibility(VISIBLE);
            // mBinding.readerTitle.juzSpinner.setVisibility(GONE);
        }

        // mBinding.readerTitle.verseSpinner.setVisibility(VISIBLE);
        mBinding.readerTitle.juzChapterVerseSpinner.setVisibility(VISIBLE);
    }

    public void setActivity(ActivityReader activity) {
        mActivity = activity;
        mReaderParams = activity.mReaderParams;
        Navigator mNavigator = activity.mNavigator;

        mJCVSelectorView.setActivity(mActivity);
    }

    public void selectJuzIntoSpinner(int juzNo) {
        mReaderParams.currJuzNo = juzNo;

        if (mJuzAdapter != null) {
            final JuzSpinnerItem selectedItem = mJuzAdapter.getSelectedItem();
            if (selectedItem != null && selectedItem.getJuzNumber() == juzNo) {
                return;
            }

            for (int i = 0, l = mJuzAdapter.getItemCount(); i < l; i++) {
                final JuzSpinnerItem item = mJuzAdapter.getItem(i);
                if (item.getJuzNumber() == juzNo) {
                    mJuzAdapter.selectSansInvocation(i);
                    break;
                }
            }
        }
    }

    public void selectChapterIntoSpinner(int chapterNo) {
        if (mChapterAdapter != null) {
            final ChapterSpinnerItem selectedItem = mChapterAdapter.getSelectedItem();
            if (selectedItem != null && selectedItem.getChapter().getChapterNumber() == chapterNo) {
                return;
            }

            for (int i = 0, l = mChapterAdapter.getItemCount(); i < l; i++) {
                final ChapterSpinnerItem item = mChapterAdapter.getItem(i);
                if (item.getChapter().getChapterNumber() == chapterNo) {
                    mChapterAdapter.selectSansInvocation(i);
                    break;
                }
            }
        }
    }

    public void selectVerseIntoSpinner(int chapterNo, int verseNo) {
        if (mReaderParams.currChapter != null) {
            mReaderParams.currChapter.setCurrentVerseNo(verseNo);
        }

        if (mVerseAdapter != null) {
            final VerseSpinnerItem selectedItem = mVerseAdapter.getSelectedItem();
            if (selectedItem != null && selectedItem.getChapterNo() == chapterNo && selectedItem.getVerseNo() == verseNo) {
                return;
            }

            for (int i = 0, l = mVerseAdapter.getItemCount(); i < l; i++) {
                final VerseSpinnerItem item = mVerseAdapter.getItem(i);
                if (item.getChapterNo() == chapterNo && item.getVerseNo() == verseNo) {
                    mVerseAdapter.selectSansInvocation(i);
                    break;
                }
            }
        }
    }

    @Override
    public void destroy() {
        // mJuzSpinner.closePopup();
        // mChapterSpinner.closePopup();
        // mVerseSpinner.closePopup();
        mJCVSelectorView.closePopup();
    }
}
