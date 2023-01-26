/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.activities;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.peacedesign.android.utils.DrawableUtils;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.BuildConfig;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.databinding.ActivityAboutBinding;
import com.quranapp.android.databinding.LytReaderSettingsItemBinding;
import com.quranapp.android.utils.app.InfoUtils;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.widgets.IconedTextView;

public class ActivityAbout extends BaseActivity {
    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_about;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        ActivityAboutBinding binding = ActivityAboutBinding.bind(activityView);
        initHeader(binding.header);
        init(binding);
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(this::onBackPressed);
        header.setTitleText(R.string.strTitleAboutUs);

        header.setShowRightIcon(false);
        header.setShowSearchIcon(false);

        header.setBGColor(R.color.colorBGPage);
    }

    private void init(ActivityAboutBinding binding) {
        LytReaderSettingsItemBinding versionBinding = LytReaderSettingsItemBinding.inflate(getLayoutInflater());
        setup(binding, versionBinding, R.drawable.dr_logo, R.string.strTitleAppVersion, BuildConfig.VERSION_NAME, false);

        LytReaderSettingsItemBinding aboutUsBinding = LytReaderSettingsItemBinding.inflate(getLayoutInflater());
        setup(binding, aboutUsBinding, R.drawable.dr_icon_info, R.string.strTitleAboutUs, null, true);

        LytReaderSettingsItemBinding licensesBinding = LytReaderSettingsItemBinding.inflate(getLayoutInflater());
        setup(binding, licensesBinding, R.drawable.dr_icon_article, R.string.strTitleLicenses, null, true);

        aboutUsBinding.getRoot().setOnClickListener(v -> InfoUtils.openAbout(this));
        licensesBinding.getRoot().setOnClickListener(v -> {
            OssLicensesMenuActivity.setActivityTitle(str(R.string.strTitleLicenses));
            startActivity(new Intent(this, OssLicensesMenuActivity.class));
        });
    }

    private void setup(
            ActivityAboutBinding parent, LytReaderSettingsItemBinding binding,
            int startIcon, int titleRes, String subtitle, boolean showArrow
    ) {
        setupLauncherParams(binding.getRoot());
        prepareTitle(binding, titleRes, subtitle);
        setupIcons(startIcon, binding.launcher, showArrow);

        parent.container.addView(binding.getRoot());
    }

    private void prepareTitle(LytReaderSettingsItemBinding binding, int titleRes, String subtitle) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int flag = SPAN_EXCLUSIVE_EXCLUSIVE;

        String title = str(titleRes);
        SpannableString titleSS = new SpannableString(title);
        titleSS.setSpan(new StyleSpan(Typeface.BOLD), 0, titleSS.length(), flag);
        ssb.append(titleSS);

        if (!TextUtils.isEmpty(subtitle)) {
            SpannableString subtitleSS = new SpannableString(subtitle);
            subtitleSS.setSpan(new AbsoluteSizeSpan(dimen(R.dimen.dmnCommonSize2)), 0, subtitleSS.length(), flag);
            subtitleSS.setSpan(new ForegroundColorSpan(color(R.color.colorText3)), 0, subtitleSS.length(), flag);
            ssb.append("\n").append(subtitleSS);
        }

        binding.launcher.setText(ssb);
    }

    private void setupIcons(int startIconRes, IconedTextView textView, boolean showArrow) {
        Context context = textView.getContext();
        Drawable chevronRight = showArrow ? ResUtils.getDrawable(context, R.drawable.dr_icon_chevron_right) : null;

        if (chevronRight != null && WindowUtils.isRTL(context)) {
            chevronRight = DrawableUtils.rotate(context, chevronRight, 180);
        }

        textView.setDrawables(ResUtils.getDrawable(context, startIconRes), null, chevronRight, null);
    }

    private void setupLauncherParams(View launcherView) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        ViewUtils.setMarginVertical(params, dp2px(5));
        ViewUtils.setMarginHorizontal(params, dp2px(10));
        launcherView.setLayoutParams(params);
    }
}
