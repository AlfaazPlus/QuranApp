/*
 * Created by Faisal Khan on (c) 27/8/2021.
 */

package com.quranapp.android.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.quranapp.android.R;
import com.quranapp.android.databinding.LytBoldHeaderBinding;
import com.quranapp.android.databinding.LytSimpleSearchBoxBinding;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewKt;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;

public class BoldHeader extends AppBarLayout {
    private final LytBoldHeaderBinding mBinding;
    private BoldHeaderCallback mCallback;

    public BoldHeader(@NonNull Context context) {
        this(context, null);
    }

    public BoldHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoldHeader(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBinding = LytBoldHeaderBinding.inflate(LayoutInflater.from(context), this, false);
        addView(mBinding.getRoot());


        init(mBinding);
    }

    private void init(LytBoldHeaderBinding binding) {
        initThis();

        TooltipCompat.setTooltipText(mBinding.searchIcon, getContext().getString(R.string.strHintSearch));

        binding.back.setOnClickListener(v -> {
            if (checkSearchShown()) {
                return;
            }

            if (mCallback != null) {
                mCallback.onBackIconClick();
            }
        });
        binding.searchIcon.setOnClickListener(v -> {
            if (binding.searchIcon.getVisibility() == VISIBLE) {
                toggleSearchBox(binding.search, true);
            }
        });
        binding.rightIcon.setOnClickListener(v -> {
            if (binding.rightIcon.getVisibility() == VISIBLE) {
                if (mCallback != null) {
                    mCallback.onRightIconClick();
                }
            }
        });

        binding.search.searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (binding.searchIcon.getVisibility() != VISIBLE) {
                return true;
            }
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (mCallback != null) {
                    mCallback.onSearchRequest(binding.search.searchBox, v.getText());
                }
            }
            return true;
        });
        binding.search.searchBox.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(@NonNull CharSequence s, int start, int before, int count) {
                if (binding.searchIcon.getVisibility() != VISIBLE) {
                    return;
                }
                if (mCallback != null) {
                    mCallback.onSearchRequest(binding.search.searchBox, s);
                }
            }
        });
    }

    private void initThis() {
        setBackgroundColor(Color.TRANSPARENT);

        mBinding.search.searchBox.setPaddingRelative(
            ContextKt.dp2px(getContext(), 5),
            mBinding.search.searchBox.getPaddingTop(),
            mBinding.search.searchBox.getPaddingEnd(),
            mBinding.search.searchBox.getPaddingBottom()
        );
    }

    private void toggleSearchBox(LytSimpleSearchBoxBinding searchBoxBinding, boolean showSearch) {
        searchBoxBinding.getRoot().setVisibility(showSearch ? VISIBLE : GONE);

        EditText searchBox = searchBoxBinding.searchBox;

        InputMethodManager imm = (InputMethodManager) searchBox.getContext().getSystemService(
            Context.INPUT_METHOD_SERVICE);

        if (showSearch) {
            searchBox.requestFocus();
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
        } else {
            searchBox.clearFocus();
            searchBox.setText(null);
            imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        }
    }

    public void toggleSearchBox(boolean showSearchBox) {
        toggleSearchBox(mBinding.search, showSearchBox);
    }

    public void setLeftIconRes(@DrawableRes int iconRes) {
        mBinding.back.setImageResource(iconRes);
    }

    public void setRightIconRes(@DrawableRes int iconRes) {
        setRightIconRes(iconRes, null);
    }

    public void setRightIconRes(@DrawableRes int iconRes, CharSequence tooltipText) {
        mBinding.rightIcon.setImageResource(iconRes);
        TooltipCompat.setTooltipText(mBinding.rightIcon, tooltipText);
    }

    public void setTitleText(CharSequence titleText) {
        mBinding.titleText.setText(titleText);
    }

    public void setTitleText(@StringRes int titleTextRes) {
        setTitleText(getContext().getText(titleTextRes));
    }

    public void setSearchHint(CharSequence hintText) {
        mBinding.search.searchBox.setHint(hintText);
    }

    public void setSearchHint(@StringRes int hintTextRes) {
        setSearchHint(getContext().getText(hintTextRes));
    }

    public void setBGColor(@ColorRes int colorRes) {
        int color = ContextCompat.getColor(getContext(), colorRes);
        mBinding.search.getRoot().setBackgroundColor(color);
        setBackgroundColor(color);
    }

    public void setShowSearchIcon(boolean showSearchIcon) {
        mBinding.searchIcon.setVisibility(showSearchIcon ? VISIBLE : GONE);
    }

    public void setShowRightIcon(boolean showRightIcon) {
        mBinding.rightIcon.setVisibility(showRightIcon ? VISIBLE : GONE);
    }

    public void disableRightBtn(boolean disable) {
        ViewKt.disableView(mBinding.rightIcon, disable);
    }

    public void setCallback(BoldHeaderCallback callback) {
        mCallback = callback;
    }

    public boolean isSearchShowing() {
        return mBinding.search.getRoot().getVisibility() == VISIBLE;
    }

    public boolean checkSearchShown() {
        if (isSearchShowing()) {
            toggleSearchBox(false);
            return true;
        }

        return false;
    }

    public interface BoldHeaderCallback {
        void onBackIconClick();

        default void onRightIconClick() {
        }

        default void onSearchRequest(EditText searchBox, CharSequence newText) {
        }
    }
}
