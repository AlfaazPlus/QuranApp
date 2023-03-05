package com.quranapp.android.utils.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivitySearch;
import com.quranapp.android.databinding.LytSearchFiltersBinding;

import kotlin.Unit;

public class SearchFilters {
    private final ActivitySearch mActivitySearch;
    private PeaceDialog mFiltersDialog;
    private LytSearchFiltersBinding mBinding;

    public boolean searchWordPart;
    public boolean searchInArText;
    public boolean showQuickLinks = true;

    public String selectedTranslSlug;

    private boolean mTmpSearchWordPart;
    private boolean mTmpSearchInArText;

    public SearchFilters(ActivitySearch activitySearch, String initiallySelectedSlug) {
        mActivitySearch = activitySearch;
        selectedTranslSlug = initiallySelectedSlug;

        initFilters(mActivitySearch);
    }


    private void initFilters(Context context) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(context);
        builder.setTitle(R.string.strTitleFilters);
        builder.setNegativeButton(R.string.strLabelCancel, null);
        builder.setPositiveButton(R.string.strLabelApply, (dialog, which) -> {
            if (commitChanges()) {
                mActivitySearch.applyFilters();
            }
        });
        builder.setFocusOnPositive(true);
        builder.setView(prepareFiltersLayout(context));
        mFiltersDialog = builder.create();
    }

    private View prepareFiltersLayout(Context context) {
        mBinding = LytSearchFiltersBinding.inflate(LayoutInflater.from(context));
        /*setupTranslsSpinner(mBinding.translsSpinner);*/
        setupCheckBoxes();
        return mBinding.getRoot();
    }

    private void setupCheckBoxes() {
        mBinding.searchWordPart.setOnCheckChangedListener((buttonView, isChecked) -> {
            mTmpSearchWordPart = isChecked;
            return Unit.INSTANCE;
        });
    }

    private boolean commitChanges() {
        if (searchWordPart == mTmpSearchWordPart && searchInArText == mTmpSearchInArText) {
            return false;
        }

        searchWordPart = mTmpSearchWordPart;
        searchInArText = mTmpSearchInArText;

        return true;
    }

    private void preShow() {
        mBinding.searchWordPart.setChecked(searchWordPart);
    }

    public void show() {
        preShow();
        mFiltersDialog.show();
    }
}
