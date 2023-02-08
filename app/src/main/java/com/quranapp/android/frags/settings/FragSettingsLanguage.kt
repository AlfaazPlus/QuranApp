/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import static com.peacedesign.android.widget.compound.PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_LEFT;
import static com.peacedesign.android.widget.compound.PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_RIGHT;
import static com.peacedesign.android.widget.compound.PeaceCompoundButton.COMPOUND_TEXT_LEFT;
import static com.quranapp.android.utils.sp.SPAppConfigs.LOCALE_DEFAULT;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.radio.PeaceRadioButton;
import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.databinding.FragSettingsLangBinding;
import com.quranapp.android.utils.IntentUtils;
import com.quranapp.android.utils.sp.SPAppConfigs;
import com.quranapp.android.views.BoldHeader;

import java.util.Locale;
import java.util.regex.Pattern;

public class FragSettingsLanguage extends FragSettingsBase {
    private FragSettingsLangBinding mBinding;
    private String initLocale;

    @Override
    public String getFragTitle(Context ctx) {
        return ctx.getString(R.string.strTitleAppLanguage);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.frag_settings_lang;
    }

    @Override
    public void setupHeader(ActivitySettings activity, BoldHeader header) {
        super.setupHeader(activity, header);
        header.setCallback(activity::onBackPressed);
        header.setShowSearchIcon(false);
        header.setShowRightIcon(false);
    }

    @Override
    protected boolean shouldCreateScroller() {
        return true;
    }

    @Override
    public void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding = FragSettingsLangBinding.bind(view);

        initLocale = SPAppConfigs.getLocale(ctx);
        init(ctx);
    }

    private void init(Context ctx) {
        initLanguage(ctx);
    }

    @SuppressLint("RtlHardcoded")
    private void initLanguage(Context ctx) {
        String[] availableLocales = {LOCALE_DEFAULT, "en", "hi", "ur"};
        String[] availableLocaleLabels = strArray(ctx, R.array.arrLocales);

        int forcedTextGravity = WindowUtils.isRTL(ctx)
                ? COMPOUND_TEXT_GRAVITY_RIGHT
                : COMPOUND_TEXT_GRAVITY_LEFT;
        int preCheckedId = View.NO_ID;
        for (int i = 0, l = availableLocales.length; i < l; i++) {
            PeaceRadioButton radio = new PeaceRadioButton(ctx);

            String locale = availableLocales[i];
            radio.setTag(locale);

            radio.setCompoundDirection(COMPOUND_TEXT_LEFT);
            radio.setBackgroundResource(R.drawable.dr_bg_hover);
            radio.setSpaceBetween(dp2px(ctx, 20));
            radio.setTextAppearance(R.style.TextAppearanceCommonTitle);
            radio.setForceTextGravity(forcedTextGravity);

            ViewUtils.setPaddingHorizontal(radio, dp2px(ctx, 20));
            ViewUtils.setPaddingVertical(radio, dp2px(ctx, 12));

            radio.setTexts(availableLocaleLabels[i], null);

            mBinding.list.addView(radio);

            if (locale.equals(initLocale)) {
                preCheckedId = radio.getId();
            }
        }

        if (preCheckedId != View.NO_ID) {
            mBinding.list.check(preCheckedId);
        }

        mBinding.list.setOnCheckedChangedListener((button, checkedId) -> {
            String locale = (String) button.getTag();
            SPAppConfigs.setLocale(ctx, locale);

            Intent intent = new Intent(IntentUtils.INTENT_ACTION_APP_LANGUAGE_CHANGED);
            intent.putExtra("locale", locale);
            mBinding.getRoot().getContext().sendBroadcast(intent);
        });
    }

    private void search(CharSequence query) {
        boolean isEmpty = TextUtils.isEmpty(query);
        Pattern pattern = Pattern.compile(String.valueOf(query), CASE_INSENSITIVE | DOTALL);

        for (int i = 0, l = mBinding.list.getChildCount(); i < l; i++) {
            PeaceRadioButton radio = (PeaceRadioButton) mBinding.list.getChildAt(i);

            if (radio != null) {
                if (isEmpty || pattern.matcher(radio.getText()).find()) {
                    radio.setVisibility(View.VISIBLE);
                } else {
                    radio.setVisibility(View.GONE);
                }
            }
        }
    }

    private void changeCheckpoint() {
        int checkedId = mBinding.list.getCheckedRadioId();
        View button = mBinding.list.findViewById(checkedId);
        if (button == null) return;

        String locale = (String) button.getTag();
        if (locale == null || TextUtils.equals(locale, initLocale)) return;

        PeaceDialog.newBuilder(mBinding.getRoot().getContext())
                .setTitle(R.string.strTitleChangeLanguage)
                .setMessage(R.string.strMsgChangeLanguage)
                .setNegativeButton(R.string.strLabelCancel, null)
                .setPositiveButton(R.string.strLabelChangeNRestart, (dialog1, which) -> restartApp(mBinding.list.getContext(), locale))
                .setButtonsDirection(PeaceDialog.STACKED)
                .setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                .setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                .setDialogGravity(PeaceDialog.GRAVITY_BOTTOM)
                .setFocusOnPositive(true).show();
    }

    private void restartApp(Context ctx, String locale) {
        SPAppConfigs.setLocale(ctx, new Locale(locale).toLanguageTag());
        restartMainActivity(ctx);
    }
}
