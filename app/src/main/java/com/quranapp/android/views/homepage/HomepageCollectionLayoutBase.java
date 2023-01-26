/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 28/2/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.homepage;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.peacedesign.android.utils.Dimen.dp2px;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.CallSuper;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.touchutils.HoverPushOpacityEffect;
import com.quranapp.android.R;
import com.quranapp.android.databinding.LytHomepageTitledItemTitleBinding;
import com.quranapp.android.utils.extended.GapedItemDecoration;

public abstract class HomepageCollectionLayoutBase extends LinearLayout {
    public HomepageCollectionLayoutBase(Context context) {
        this(context, null);
    }

    public HomepageCollectionLayoutBase(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomepageCollectionLayoutBase(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        post(() -> {
            initThis(context);
            initTitle(context);
        });
    }

    private void initThis(Context context) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.colorBGHomePageItem));
        setOrientation(VERTICAL);
        ViewUtils.setPaddingVertical(this, dp2px(context, 10));
    }

    private void initTitle(Context context) {
        LytHomepageTitledItemTitleBinding binding = LytHomepageTitledItemTitleBinding.inflate(LayoutInflater.from(context));

        int headerIcon = getHeaderIcon();
        if (headerIcon != 0) {
            binding.titleIcon.setImageResource(headerIcon);
        }

        int headerTitle = getHeaderTitle();
        if (headerTitle != 0) {
            binding.titleText.setText(headerTitle);
        }

        if (showViewAllBtn()) {
            binding.viewAll.setVisibility(VISIBLE);
            binding.viewAll.setOnTouchListener(new HoverPushOpacityEffect());
            binding.viewAll.setOnClickListener(v -> onViewAllClick(v.getContext()));
        } else {
            binding.viewAll.setVisibility(GONE);
        }

        setupHeader(context, binding);

        addView(binding.getRoot(), 0);
    }

    protected void setupHeader(Context context, LytHomepageTitledItemTitleBinding header) {
    }

    protected boolean showViewAllBtn() {
        return false;
    }

    protected void onViewAllClick(Context context) {
    }

    @StringRes
    protected abstract int getHeaderTitle();

    @DrawableRes
    protected abstract int getHeaderIcon();

    protected void showLoader() {
        if (findViewById(R.id.loader) != null) {
            return;
        }

        Context ctx = getContext();

        ProgressBar loader = new ProgressBar(ctx);
        loader.setId(R.id.loader);

        ViewUtils.setPaddings(loader, dp2px(ctx, 15));

        int size = dp2px(ctx, 35);
        addView(loader, new LayoutParams(size, size));
    }

    protected void hideLoader() {
        View loader = findViewById(R.id.loader);
        if (loader != null) {
            ViewUtils.removeView(loader);
        }
    }

    @CallSuper
    protected RecyclerView resolveListView() {
        RecyclerView list = findViewById(R.id.list);
        if (list == null) {
            list = makeRecView(getContext());
            addView(list);
        }
        return list;
    }

    private RecyclerView makeRecView(Context ctx) {
        RecyclerView recyclerView = new RecyclerView(ctx);
        recyclerView.setId(R.id.list);
        ViewUtils.setBounceOverScrollRV(recyclerView);
        ViewUtils.setPaddings(recyclerView, dp2px(ctx, 10));
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        recyclerView.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false));
        recyclerView.addItemDecoration(new GapedItemDecoration(dp2px(ctx, 5)));

        return recyclerView;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof MarginLayoutParams) {
            ViewUtils.setMarginVertical((MarginLayoutParams) params, dp2px(getContext(), 3));
        }
        super.setLayoutParams(params);
    }
}
